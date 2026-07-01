package com.sms.system.service.impl.notification;

import com.sms.common.exception.ServiceException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.SysDepartment;
import com.sms.system.entity.SysSchoolFamilyContact;
import com.sms.system.entity.SysSchoolDepartmentMember;
import com.sms.system.entity.dto.NotificationReceiverSaveDTO;
import com.sms.system.entity.notification.NotificationReceiver;
import com.sms.system.entity.notification.receiver.ReceiverResolutionContext;
import com.sms.system.entity.vo.NotificationReceiverVO;
import com.sms.system.entity.vo.ResolvedReceiversVO;
import com.sms.system.mapper.SysDepartmentMapper;
import com.sms.system.mapper.SysSchoolFamilyContactMapper;
import com.sms.system.mapper.SysSchoolDepartmentMemberMapper;
import com.sms.system.mapper.notification.NotificationReceiverMapper;
import com.sms.system.service.impl.SysSchoolDepartmentServiceImpl;
import com.sms.system.service.notification.INotificationReceiverService;
import com.sms.common.utils.bean.BeanCopyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * 通知接收對象 Service。
 */
@Service
public class NotificationReceiverServiceImpl implements INotificationReceiverService {

    private static final Logger log = LoggerFactory.getLogger(NotificationReceiverServiceImpl.class);

    private static final String RECEIVE_TYPE_DEPARTMENT = "1";
    private static final String RECEIVE_TYPE_PERSONAL = "2";
    private static final Integer TARGET_TYPE_WECOM = 1;
    private static final Integer TARGET_TYPE_CUSTOM = 2;

    @Autowired
    private NotificationReceiverMapper notificationReceiverMapper;

    @Autowired
    private SysSchoolFamilyContactMapper schoolFamilyContactMapper;

    @Autowired
    private SysSchoolDepartmentMemberMapper schoolDepartmentMemberMapper;

    @Autowired
    private SysDepartmentMapper sysDepartmentMapper;

    @Autowired
    private SysSchoolDepartmentServiceImpl sysSchoolDepartmentService;

    @Override
    public List<NotificationReceiverVO> selectByNotificationId(Long notificationId) {
        return BeanCopyUtils.copyList(notificationReceiverMapper.selectByNotificationId(notificationId),
                NotificationReceiverVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int save(NotificationReceiverSaveDTO notificationReceiverSaveDTO) {
        NotificationReceiver receiver = BeanCopyUtils.copy(notificationReceiverSaveDTO, NotificationReceiver.class);
        if (receiver.getCreateTime() == null) {
            receiver.setCreateTime(LocalDateTime.now());
        }
        return notificationReceiverMapper.insert(receiver);
    }

    @Override
    public ResolvedReceiversVO resolveReceivers(List<NotificationReceiver> receivers, boolean strictDepartmentCheck) {
        ReceiverResolutionContext context = new ReceiverResolutionContext();

        if (receivers == null || receivers.isEmpty()) {
            log.warn("notification receivers are empty");
            return context.toResult();
        }

        try {
            for (NotificationReceiver receiver : receivers) {
                parseReceiver(receiver, context, strictDepartmentCheck);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("failed to resolve notification receivers", e);
            throw new ServiceException("解析接收對象失敗：" + e.getMessage());
        }

        validateCustomMemberStudentUserId(context, strictDepartmentCheck);
        List<SysSchoolFamilyContact> uniqueRelations = deduplicateRelations(context.relations);

        log.info("解析完成 - parentUserIds: {}, studentUserIds: {}, 關係數: {} (去重前: {})",
                context.parentUserIds.size(), context.studentUserIds.size(),
                uniqueRelations.size(), context.relations.size());

        return context.toResult(uniqueRelations);
    }

    private void parseReceiver(NotificationReceiver receiver, ReceiverResolutionContext context,
                               boolean strictDepartmentCheck) {
        String receiveData = receiver.getReceiveData();
        if (receiveData == null || receiveData.trim().isEmpty()) {
            return;
        }

        JSONArray dataArray = JSON.parseArray(receiveData);
        if (dataArray == null || dataArray.isEmpty()) {
            return;
        }

        for (int i = 0; i < dataArray.size(); i++) {
            parseDataItem(receiver.getReceiveType(), dataArray.getJSONObject(i), context, strictDepartmentCheck);
        }
    }

    private void parseDataItem(String receiveType, JSONObject dataItem, ReceiverResolutionContext context,
                               boolean strictDepartmentCheck) {
        Integer type = dataItem.getInteger("type");
        JSONArray ids = dataItem.getJSONArray("receive_ids");
        if (ids == null || ids.isEmpty()) {
            ids = dataItem.getJSONArray("ids");
        }
        if (type == null || ids == null || ids.isEmpty()) {
            return;
        }

        List<Long> idList = ids.toJavaList(Long.class);
        List<Long> departmentIdList = buildDepartmentIdList(dataItem, idList);

        if (RECEIVE_TYPE_PERSONAL.equals(receiveType)) {
            resolvePersonalReceivers(type, idList, departmentIdList, context, strictDepartmentCheck);
        } else if (RECEIVE_TYPE_DEPARTMENT.equals(receiveType)) {
            resolveDepartmentReceivers(type, idList, context);
        }
    }

    private List<Long> buildDepartmentIdList(JSONObject dataItem, List<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return Collections.emptyList();
        }
        JSONArray departmentIds = dataItem.getJSONArray("department_ids");
        if (departmentIds == null || departmentIds.size() != idList.size()) {
            return new ArrayList<>(Collections.nCopies(idList.size(), null));
        }
        List<Long> result = new ArrayList<>(idList.size());
        for (int i = 0; i < idList.size(); i++) {
            result.add(departmentIds.getLong(i));
        }
        return result;
    }

    private void resolvePersonalReceivers(Integer type, List<Long> idList, List<Long> departmentIdList,
                                          ReceiverResolutionContext context, boolean strictDepartmentCheck) {
        if (TARGET_TYPE_WECOM.equals(type)) {
            resolveWecomPersonalReceivers(idList, departmentIdList, context, strictDepartmentCheck);
        } else if (TARGET_TYPE_CUSTOM.equals(type)) {
            resolveCustomPersonalReceivers(idList, departmentIdList, context);
        }
    }

    private void resolveDepartmentReceivers(Integer type, List<Long> idList, ReceiverResolutionContext context) {
        if (TARGET_TYPE_WECOM.equals(type)) {
            resolveWecomDepartmentReceivers(idList, context);
        } else if (TARGET_TYPE_CUSTOM.equals(type)) {
            resolveCustomDepartmentReceivers(idList, context);
        }
    }

    private void resolveWecomPersonalReceivers(List<Long> ids, List<Long> departmentIds,
                                               ReceiverResolutionContext context, boolean strictDepartmentCheck) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Long> uniqueRelationIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (uniqueRelationIds.isEmpty()) {
            return;
        }

        List<SysSchoolFamilyContact> relations = schoolFamilyContactMapper.selectByIds(uniqueRelationIds);
        if (relations == null || relations.isEmpty()) {
            return;
        }

        Map<Long, SysSchoolFamilyContact> relationMap = relations.stream()
                .filter(relation -> relation.getId() != null)
                .collect(Collectors.toMap(SysSchoolFamilyContact::getId, relation -> relation, (a, b) -> a));

        Map<String, List<SysSchoolFamilyContact>> schoolFamilyContactsMap =
                loadSchoolFamilyContactsMap(relations);

        for (int i = 0; i < ids.size(); i++) {
            Long relationId = ids.get(i);
            if (relationId == null) {
                continue;
            }
            SysSchoolFamilyContact relation = relationMap.get(relationId);
            if (relation == null || relation.getParentUserId() == null || relation.getParentUserId().trim().isEmpty()) {
                continue;
            }

            context.parentUserIds.add(relation.getParentUserId());
            String studentUserId = relation.getStudentUserId();
            Long explicitDepartmentId = i < departmentIds.size() ? departmentIds.get(i) : null;
            SysSchoolFamilyContact resolvedRelation = resolveRelationDepartment(
                    relation, explicitDepartmentId,
                    schoolFamilyContactsMap.get(relation.getParentUserId() + "_" + studentUserId),
                    strictDepartmentCheck);
            Long departmentId = resolvedRelation != null ? resolvedRelation.getDepartmentId() : null;

            if (!tryAddRelation(context, relation.getParentUserId(), studentUserId, departmentId)) {
                continue;
            }

            if (resolvedRelation != null) {
                context.relations.add(resolvedRelation);
            } else {
                SysSchoolFamilyContact fallback = new SysSchoolFamilyContact();
                fallback.setParentUserId(relation.getParentUserId());
                fallback.setStudentUserId(studentUserId);
                fallback.setStudentName(relation.getStudentName());
                context.relations.add(fallback);
            }
        }
    }

    private Map<String, List<SysSchoolFamilyContact>> loadSchoolFamilyContactsMap(
            List<SysSchoolFamilyContact> relations) {
        List<String> studentUserIds = relations.stream()
                .map(SysSchoolFamilyContact::getStudentUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<String, List<SysSchoolFamilyContact>> schoolFamilyContactsMap = new HashMap<>();
        if (studentUserIds.isEmpty()) {
            return schoolFamilyContactsMap;
        }

        List<SysSchoolFamilyContact> existingRelations =
                schoolFamilyContactMapper.selectByStudentUserIds(studentUserIds);
        if (existingRelations == null) {
            return schoolFamilyContactsMap;
        }

        for (SysSchoolFamilyContact relation : existingRelations) {
            if (relation.getStudentUserId() != null && relation.getParentUserId() != null) {
                String key = relation.getParentUserId() + "_" + relation.getStudentUserId();
                schoolFamilyContactsMap.computeIfAbsent(key, k -> new ArrayList<>()).add(relation);
            }
        }
        return schoolFamilyContactsMap;
    }

    private SysSchoolFamilyContact resolveRelationDepartment(SysSchoolFamilyContact relation,
                                                               Long explicitDepartmentId,
                                                               List<SysSchoolFamilyContact> candidateRelations,
                                                               boolean strictDepartmentCheck) {
        if (explicitDepartmentId != null) {
            if (candidateRelations != null) {
                for (SysSchoolFamilyContact candidate : candidateRelations) {
                    if (explicitDepartmentId.equals(candidate.getDepartmentId())) {
                        return candidate;
                    }
                }
            }
            SysSchoolFamilyContact resolved = new SysSchoolFamilyContact();
            resolved.setParentUserId(relation.getParentUserId());
            resolved.setStudentUserId(relation.getStudentUserId());
            resolved.setStudentName(relation.getStudentName());
            resolved.setDepartmentId(explicitDepartmentId);
            return resolved;
        }
        if (relation.getDepartmentId() != null) {
            return relation;
        }
        if (candidateRelations == null || candidateRelations.isEmpty()) {
            return null;
        }
        if (candidateRelations.size() == 1) {
            return candidateRelations.get(0);
        }
        if (!strictDepartmentCheck) {
            log.warn("家長學生關係存在多個部門且未指定 department_id，已使用第一筆: parentUserId={}, studentUserId={}",
                    relation.getParentUserId(), relation.getStudentUserId());
            return candidateRelations.get(0);
        }
        throw new ServiceException(String.format(
                "家長「%s」存在多個班級綁定，請在選人時指定班級後再發送（relationId=%d）",
                relation.getStudentName() != null ? relation.getStudentName() : relation.getParentUserId(),
                relation.getId()));
    }

    private void resolveCustomPersonalReceivers(List<Long> ids, List<Long> departmentIds,
                                                ReceiverResolutionContext context) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Long> uniqueMemberIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (uniqueMemberIds.isEmpty()) {
            return;
        }

        List<SysSchoolDepartmentMember> members = schoolDepartmentMemberMapper.selectMembersByIds(uniqueMemberIds);
        if (members == null || members.isEmpty()) {
            return;
        }

        Map<Long, SysSchoolDepartmentMember> memberMap = members.stream()
                .filter(member -> member.getId() != null)
                .collect(Collectors.toMap(SysSchoolDepartmentMember::getId, member -> member, (a, b) -> a));

        for (int i = 0; i < ids.size(); i++) {
            Long memberId = ids.get(i);
            if (memberId == null) {
                continue;
            }
            SysSchoolDepartmentMember member = memberMap.get(memberId);
            if (member == null || member.getUserid() == null || member.getUserid().trim().isEmpty()) {
                continue;
            }
            registerCustomMember(context, member, i < departmentIds.size() ? departmentIds.get(i) : null);
        }
    }

    private void resolveWecomDepartmentReceivers(List<Long> departmentIds, ReceiverResolutionContext context) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return;
        }

        List<Long> classDepartmentIds = resolveClassDepartmentIds(departmentIds);
        log.info("解析部門家長關係 - 輸入部門 IDs: {}, 解析後班級部門 IDs: {}", departmentIds, classDepartmentIds);
        if (classDepartmentIds.isEmpty()) {
            return;
        }

        List<SysSchoolFamilyContact> relations =
                schoolFamilyContactMapper.selectByDepartmentIds(classDepartmentIds);
        if (relations == null) {
            return;
        }

        for (SysSchoolFamilyContact relation : relations) {
            if (relation.getParentUserId() == null || relation.getParentUserId().trim().isEmpty()) {
                continue;
            }
            context.parentUserIds.add(relation.getParentUserId());
            if (tryAddRelation(context, relation.getParentUserId(), relation.getStudentUserId(),
                    relation.getDepartmentId())) {
                context.relations.add(relation);
            }
        }
    }

    private void resolveCustomDepartmentReceivers(List<Long> departmentIds, ReceiverResolutionContext context) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return;
        }

        List<Long> allDescendantDepartmentIds =
                sysSchoolDepartmentService.resolveAllDescendantDepartmentIdsByType(departmentIds, 2);
        log.info("解析自定義家校部門成員 - 輸入部門 IDs: {}, 解析後所有子孫部門 IDs: {}",
                departmentIds, allDescendantDepartmentIds);
        if (allDescendantDepartmentIds.isEmpty()) {
            return;
        }

        List<SysSchoolDepartmentMember> members =
                schoolDepartmentMemberMapper.selectMembersByDepartmentIds(allDescendantDepartmentIds);
        if (members == null) {
            return;
        }

        for (SysSchoolDepartmentMember member : members) {
            if (member.getUserid() == null || member.getUserid().trim().isEmpty()) {
                continue;
            }
            registerCustomMember(context, member, null);
        }
    }

    private void registerCustomMember(ReceiverResolutionContext context, SysSchoolDepartmentMember member,
                                      Long explicitDepartmentId) {
        context.parentUserIds.add(member.getUserid());
        registerParentStudentMapping(context, member.getUserid(), member.getStudentUserId());

        Long departmentId = explicitDepartmentId != null ? explicitDepartmentId : member.getDepartmentId();
        if (departmentId != null) {
            context.studentDepartmentIds.put(member.getUserid(), departmentId);
        }
    }

    private boolean tryAddRelation(ReceiverResolutionContext context, String parentUserId,
                                   String studentUserId, Long departmentId) {
        String uniqueKey = buildRelationKey(parentUserId, studentUserId, departmentId);
        if (context.relationKeys.contains(uniqueKey)) {
            log.debug("跳過重複的關係: parentUserId={}, studentUserId={}, departmentId={}",
                    parentUserId, studentUserId, departmentId);
            return false;
        }
        context.relationKeys.add(uniqueKey);
        return true;
    }

    private List<Long> resolveClassDepartmentIds(List<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysDepartment> allDepartments = sysDepartmentMapper.selectAll();
        if (allDepartments == null || allDepartments.isEmpty()) {
            return departmentIds;
        }

        Map<Long, SysDepartment> deptMap = allDepartments.stream()
                .filter(Objects::nonNull)
                .filter(dept -> dept.getId() != null)
                .collect(Collectors.toMap(SysDepartment::getId, dept -> dept, (a, b) -> a));

        Set<Long> classDepartmentIds = new HashSet<>();
        for (Long deptId : departmentIds) {
            SysDepartment dept = deptMap.get(deptId);
            if (dept == null) {
                continue;
            }
            if (Integer.valueOf(1).equals(dept.getType())) {
                classDepartmentIds.add(deptId);
            } else {
                collectClassDepartmentIds(deptId, allDepartments, classDepartmentIds);
            }
        }
        return new ArrayList<>(classDepartmentIds);
    }

    private void collectClassDepartmentIds(Long parentId, List<SysDepartment> allDepartments,
                                           Set<Long> classDepartmentIds) {
        if (parentId == null || allDepartments == null) {
            return;
        }

        List<SysDepartment> children = allDepartments.stream()
                .filter(Objects::nonNull)
                .filter(dept -> dept.getParentId() != null)
                .filter(dept -> dept.getParentId().longValue() == parentId)
                .collect(Collectors.toList());

        for (SysDepartment child : children) {
            if (child.getId() == null) {
                continue;
            }
            if (Integer.valueOf(1).equals(child.getType())) {
                classDepartmentIds.add(child.getId());
            } else {
                collectClassDepartmentIds(child.getId(), allDepartments, classDepartmentIds);
            }
        }
    }

    private void registerParentStudentMapping(ReceiverResolutionContext context,
                                              String parentUserId, String studentUserId) {
        if (!StringUtils.hasText(parentUserId) || !StringUtils.hasText(studentUserId)) {
            return;
        }
        context.parentStudentUserIds.putIfAbsent(parentUserId.trim(), studentUserId.trim());
    }

    private void validateCustomMemberStudentUserId(ReceiverResolutionContext context,
                                                   boolean strictDepartmentCheck) {
        Set<String> coveredByRelations = context.relations.stream()
                .map(SysSchoolFamilyContact::getParentUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (String parentUserId : context.parentUserIds) {
            if (coveredByRelations.contains(parentUserId)) {
                continue;
            }
            if (context.parentStudentUserIds.containsKey(parentUserId)) {
                continue;
            }
            String message = String.format(
                    "自定義家校成員缺少 student_user_id，家長 userid=%s，請在通訊錄中補全後再發送",
                    parentUserId);
            if (strictDepartmentCheck) {
                throw new ServiceException(message);
            }
            log.warn(message);
        }
    }

    private List<SysSchoolFamilyContact> deduplicateRelations(List<SysSchoolFamilyContact> relations) {
        List<SysSchoolFamilyContact> uniqueRelations = new ArrayList<>();
        Set<String> finalRelationKeys = new HashSet<>();
        for (SysSchoolFamilyContact relation : relations) {
            if (relation.getParentUserId() == null || relation.getStudentUserId() == null) {
                continue;
            }
            String key = buildRelationKey(relation.getParentUserId(), relation.getStudentUserId(),
                    relation.getDepartmentId());
            if (!finalRelationKeys.contains(key)) {
                uniqueRelations.add(relation);
                finalRelationKeys.add(key);
            } else {
                log.warn("檢測到重複的關係，已移除: parentUserId={}, studentUserId={}, departmentId={}",
                        relation.getParentUserId(), relation.getStudentUserId(), relation.getDepartmentId());
            }
        }
        return uniqueRelations;
    }

    private String buildRelationKey(String parentUserId, String studentUserId, Long departmentId) {
        return parentUserId + "_" + studentUserId + "_" + (departmentId != null ? departmentId : "null");
    }
}
