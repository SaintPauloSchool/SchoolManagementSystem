package com.sms.system.service.impl.notification;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.common.exception.ServiceException;
import com.sms.common.utils.bean.BeanCopyUtils;
import com.sms.system.entity.SysDepartment;
import com.sms.system.entity.SysSchoolDepartment;
import com.sms.system.entity.SysSchoolDepartmentMember;
import com.sms.system.entity.SysSchoolFamilyContact;
import com.sms.system.entity.dto.NotificationReceiverSaveDTO;
import com.sms.system.entity.notification.NotificationReceiver;
import com.sms.system.entity.notification.receiver.NotificationReceiverTarget;
import com.sms.system.entity.vo.NotificationReceiverDeptGroupVO;
import com.sms.system.entity.vo.NotificationReceiverVO;
import com.sms.system.entity.vo.ResolvedReceiversVO;
import com.sms.system.enums.NotificationReceiverType;
import com.sms.system.mapper.SysDepartmentMapper;
import com.sms.system.mapper.SysSchoolDepartmentMapper;
import com.sms.system.mapper.SysSchoolDepartmentMemberMapper;
import com.sms.system.mapper.SysSchoolFamilyContactMapper;
import com.sms.system.mapper.notification.NotificationReceiverMapper;
import com.sms.system.service.notification.INotificationReceiverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知接收對象 Service。
 */
@Service
public class NotificationReceiverServiceImpl implements INotificationReceiverService {

    private static final Logger log = LoggerFactory.getLogger(NotificationReceiverServiceImpl.class);

    @Autowired
    private NotificationReceiverMapper notificationReceiverMapper;

    @Autowired
    private SysSchoolFamilyContactMapper schoolFamilyContactMapper;

    @Autowired
    private SysSchoolDepartmentMemberMapper schoolDepartmentMemberMapper;

    @Autowired
    private SysDepartmentMapper sysDepartmentMapper;

    @Autowired
    private SysSchoolDepartmentMapper schoolDepartmentMapper;

    @Override
    public List<NotificationReceiverVO> selectByNotificationId(Long notificationId) {
        List<NotificationReceiverVO> list = BeanCopyUtils.copyList(
                notificationReceiverMapper.selectByNotificationId(notificationId),
                NotificationReceiverVO.class);
        for (NotificationReceiverVO vo : list) {
            vo.setReceiveNames(resolveReceiveNames(vo.getReceiveType(), vo.getReceiveData()));
            vo.setReceiveDeptGroups(resolveReceiveDeptGroups(vo.getReceiveType(), vo.getReceiveData()));
        }
        return list;
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
        ResolvedReceiversVO.ResolutionContext context = new ResolvedReceiversVO.ResolutionContext();

        if (receivers == null || receivers.isEmpty()) {
            log.warn("通知接收者为空");
            return context.build();
        }

        try {
            for (NotificationReceiver receiver : receivers) {
                parseReceiver(receiver, context, strictDepartmentCheck);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("无法解析通知接收者", e);
            throw new ServiceException("解析接收對象失敗：" + e.getMessage());
        }

        validateCustomMemberStudentId(context, strictDepartmentCheck);

        log.info("解析完成 - parentUserIds: {}, receiverTargets: {}, relations: {}",
                context.parentUserIds.size(), context.receiverTargets.size(), context.relations.size());

        return context.build();
    }

    private void parseReceiver(NotificationReceiver receiver, ResolvedReceiversVO.ResolutionContext context,
                               boolean strictDepartmentCheck) {
        NotificationReceiverType receiveType = NotificationReceiverType.fromCode(receiver.getReceiveType());
        if (receiveType == null || !StringUtils.hasText(receiver.getReceiveData())) {
            return;
        }

        List<NotificationReceiverTarget> targets = parseReceiverTargets(receiver.getReceiveData());
        if (targets.isEmpty()) {
            return;
        }

        if (receiveType == NotificationReceiverType.WECOM) {
            resolveWecomReceivers(targets, context, strictDepartmentCheck);
        } else if (receiveType == NotificationReceiverType.CUSTOM) {
            resolveCustomReceivers(targets, context, strictDepartmentCheck);
        }
    }

    private List<NotificationReceiverTarget> parseReceiverTargets(String receiveData) {
        JSONArray array = JSONObject.parseArray(receiveData);
        if (array == null || array.isEmpty()) {
            return Collections.emptyList();
        }
        List<NotificationReceiverTarget> targets = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String parentUserId = item.getString("parentUserId");
            if (!StringUtils.hasText(parentUserId)) {
                continue;
            }
            NotificationReceiverTarget target = new NotificationReceiverTarget();
            target.setParentUserId(parentUserId.trim());
            String studentId = item.getString("studentId");
            if (StringUtils.hasText(studentId)) {
                target.setStudentId(studentId.trim());
            }
            Long departmentId = item.getLong("departmentId");
            if (departmentId != null) {
                target.setDepartmentId(departmentId);
            }
            Long schoolDepartmentId = item.getLong("schoolDepartmentId");
            if (schoolDepartmentId != null) {
                target.setSchoolDepartmentId(schoolDepartmentId);
            }
            targets.add(target);
        }
        return targets;
    }

    /**
     * 解析 WeCom 家校通訊錄接收人。
     * <p>{@code departmentId} 以選人框傳入為準；僅額外查庫用於個性化正文。</p>
     */
    private void resolveWecomReceivers(List<NotificationReceiverTarget> targets,
                                       ResolvedReceiversVO.ResolutionContext context,
                                       boolean strictDepartmentCheck) {
        List<String> uniqueIds = distinctNonBlankIds(targets.stream()
                .map(NotificationReceiverTarget::getParentUserId)
                .collect(Collectors.toList()));

        Map<String, SysSchoolFamilyContact> contactByParentDept = Collections.emptyMap();
        if (!uniqueIds.isEmpty()) {
            List<SysSchoolFamilyContact> contacts = schoolFamilyContactMapper.selectByParentUserIds(uniqueIds);
            contactByParentDept = indexContactsByParentAndDept(contacts);
        }

        for (NotificationReceiverTarget target : targets) {
            registerTarget(context, target, strictDepartmentCheck, contactByParentDept);
        }
    }

    private void registerTarget(ResolvedReceiversVO.ResolutionContext context,
                                NotificationReceiverTarget target,
                                boolean strictDepartmentCheck,
                                Map<String, SysSchoolFamilyContact> contactByParentDept) {
        if (!StringUtils.hasText(target.getParentUserId())) {
            return;
        }
        if (strictDepartmentCheck && target.getDepartmentId() == null) {
            throw new ServiceException(String.format(
                    "接收對象缺少班級部門 ID，家長 userid=%s，請重新選擇", target.getParentUserId()));
        }

        String dedupeKey = buildTargetKey(target);
        if (!context.relationKeys.add(dedupeKey)) {
            return;
        }

        context.parentUserIds.add(target.getParentUserId());

        if (contactByParentDept != null && target.getDepartmentId() != null) {
            SysSchoolFamilyContact relation = contactByParentDept.get(
                    parentDeptKey(target.getParentUserId(), target.getDepartmentId()));
            if (relation != null) {
                context.relations.add(relation);
            }
        }

        context.receiverTargets.add(copyTarget(target));
    }

    private NotificationReceiverTarget copyTarget(NotificationReceiverTarget source) {
        NotificationReceiverTarget copy = new NotificationReceiverTarget(source.getParentUserId(), source.getStudentId());
        copy.setDepartmentId(source.getDepartmentId());
        copy.setSchoolDepartmentId(source.getSchoolDepartmentId());
        return copy;
    }

    private Map<String, SysSchoolFamilyContact> indexContactsByParentAndDept(List<SysSchoolFamilyContact> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            return Collections.emptyMap();
        }
        return contacts.stream()
                .filter(c -> StringUtils.hasText(c.getParentUserId()) && c.getDepartmentId() != null)
                .collect(Collectors.toMap(
                        c -> parentDeptKey(c.getParentUserId(), c.getDepartmentId()),
                        c -> c,
                        (left, right) -> left));
    }

    private void resolveCustomReceivers(List<NotificationReceiverTarget> targets,
                                        ResolvedReceiversVO.ResolutionContext context,
                                        boolean strictDepartmentCheck) {
        for (NotificationReceiverTarget target : targets) {
            registerTarget(context, target, strictDepartmentCheck, Collections.emptyMap());
        }
    }

    private String parentDeptKey(String parentUserId, Long departmentId) {
        return parentUserId + "_" + departmentId;
    }

    private String buildTargetKey(NotificationReceiverTarget target) {
        return target.getParentUserId() + "_"
                + (StringUtils.hasText(target.getStudentId()) ? target.getStudentId() : "null") + "_"
                + (target.getDepartmentId() != null ? target.getDepartmentId() : "null");
    }

    private void validateCustomMemberStudentId(ResolvedReceiversVO.ResolutionContext context,
                                               boolean strictDepartmentCheck) {
        Set<String> wecomParentDeptKeys = context.relations.stream()
                .filter(r -> r.getParentUserId() != null && r.getDepartmentId() != null)
                .map(r -> parentDeptKey(r.getParentUserId(), r.getDepartmentId()))
                .collect(Collectors.toSet());

        for (NotificationReceiverTarget target : context.receiverTargets) {
            if (target.getDepartmentId() != null
                    && wecomParentDeptKeys.contains(parentDeptKey(target.getParentUserId(), target.getDepartmentId()))) {
                continue;
            }
            if (StringUtils.hasText(target.getStudentId())) {
                continue;
            }
            String message = String.format(
                    "自定義家校接收對象缺少 student_id，家長 userid=%s，請在選人時確認學籍綁定",
                    target.getParentUserId());
            if (strictDepartmentCheck) {
                throw new ServiceException(message);
            }
            log.warn(message);
        }
    }

    private List<String> resolveReceiveNames(String receiveTypeCode, String receiveData) {
        NotificationReceiverType receiveType = NotificationReceiverType.fromCode(receiveTypeCode);
        if (!StringUtils.hasText(receiveData) || receiveType == null) {
            return Collections.emptyList();
        }
        try {
            List<NotificationReceiverTarget> targets = parseReceiverTargets(receiveData);
            if (targets.isEmpty()) {
                return Collections.emptyList();
            }
            Map<String, String> nameMap = buildReceiveNameMap(targets, receiveType);
            List<String> names = new ArrayList<>(targets.size());
            for (NotificationReceiverTarget target : targets) {
                String name = lookupReceiveName(nameMap, target);
                names.add(StringUtils.hasText(name) ? name : "");
            }
            return names;
        } catch (Exception e) {
            log.error("解析接收對象名稱失敗: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<NotificationReceiverDeptGroupVO> resolveReceiveDeptGroups(String receiveTypeCode, String receiveData) {
        NotificationReceiverType receiveType = NotificationReceiverType.fromCode(receiveTypeCode);
        if (!StringUtils.hasText(receiveData) || receiveType == null) {
            return Collections.emptyList();
        }
        try {
            List<NotificationReceiverTarget> targets = parseReceiverTargets(receiveData);
            if (targets.isEmpty()) {
                return Collections.emptyList();
            }

            Map<Long, List<NotificationReceiverTarget>> targetsByDept = new LinkedHashMap<>();
            List<NotificationReceiverTarget> ungroupedTargets = new ArrayList<>();
            Map<String, Long> customSchoolDeptFallback = receiveType == NotificationReceiverType.CUSTOM
                    ? buildCustomSchoolDepartmentFallbackMap(targets)
                    : Collections.emptyMap();

            for (NotificationReceiverTarget target : targets) {
                Long groupDepartmentId = resolveGroupDepartmentId(target, receiveType, customSchoolDeptFallback);
                if (groupDepartmentId != null) {
                    targetsByDept.computeIfAbsent(groupDepartmentId, key -> new ArrayList<>()).add(target);
                } else {
                    ungroupedTargets.add(target);
                }
            }

            Map<String, String> nameMap = buildReceiveNameMap(targets, receiveType);
            Map<Long, String> departmentNameMap = buildDepartmentNameMap(targetsByDept.keySet(), receiveType);
            List<NotificationReceiverDeptGroupVO> groups = new ArrayList<>();
            for (Map.Entry<Long, List<NotificationReceiverTarget>> entry : targetsByDept.entrySet()) {
                NotificationReceiverDeptGroupVO group = new NotificationReceiverDeptGroupVO();
                group.setDepartmentId(entry.getKey());
                group.setDepartmentName(departmentNameMap.getOrDefault(entry.getKey(), "未知部門"));
                List<String> names = resolveTargetNames(entry.getValue(), nameMap);
                group.setNames(names);
                group.setCount(names.size());
                groups.add(group);
            }

            groups.sort((a, b) -> {
                String nameA = a.getDepartmentName() != null ? a.getDepartmentName() : "";
                String nameB = b.getDepartmentName() != null ? b.getDepartmentName() : "";
                return nameA.compareTo(nameB);
            });

            if (!ungroupedTargets.isEmpty()) {
                NotificationReceiverDeptGroupVO ungrouped = new NotificationReceiverDeptGroupVO();
                List<String> names = resolveTargetNames(ungroupedTargets, nameMap);
                ungrouped.setDepartmentName("未分組");
                ungrouped.setNames(names);
                ungrouped.setCount(names.size());
                groups.add(ungrouped);
            }
            return groups;
        } catch (Exception e) {
            log.error("解析接收對象部門分組失敗: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private Long resolveGroupDepartmentId(NotificationReceiverTarget target,
                                          NotificationReceiverType receiveType,
                                          Map<String, Long> customSchoolDeptFallback) {
        if (receiveType == NotificationReceiverType.CUSTOM) {
            if (target.getSchoolDepartmentId() != null) {
                return target.getSchoolDepartmentId();
            }
            if (customSchoolDeptFallback != null && !customSchoolDeptFallback.isEmpty()) {
                String deptKey = parentDeptKey(target.getParentUserId(), target.getDepartmentId());
                Long fallbackId = customSchoolDeptFallback.get(deptKey);
                if (fallbackId != null) {
                    return fallbackId;
                }
            }
            return null;
        }
        return target.getDepartmentId();
    }

    private Map<String, Long> buildCustomSchoolDepartmentFallbackMap(List<NotificationReceiverTarget> targets) {
        List<String> userids = targets.stream()
                .filter(target -> target.getSchoolDepartmentId() == null)
                .map(NotificationReceiverTarget::getParentUserId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (userids.isEmpty()) {
            return Collections.emptyMap();
        }

        List<SysSchoolDepartmentMember> members = schoolDepartmentMemberMapper.selectMembersByUserids(userids);
        if (members == null || members.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Long> fallbackMap = new HashMap<>();
        for (SysSchoolDepartmentMember member : members) {
            if (!StringUtils.hasText(member.getUserid()) || member.getSchoolDepartmentId() == null) {
                continue;
            }
            if (member.getDepartmentId() != null) {
                fallbackMap.putIfAbsent(
                        parentDeptKey(member.getUserid(), member.getDepartmentId()),
                        member.getSchoolDepartmentId());
            }
            fallbackMap.putIfAbsent(member.getUserid(), member.getSchoolDepartmentId());
        }
        return fallbackMap;
    }

    private Map<Long, String> buildDepartmentNameMap(Set<Long> departmentIds, NotificationReceiverType receiveType) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> nameMap = new HashMap<>();
        if (receiveType == NotificationReceiverType.WECOM) {
            List<SysDepartment> departments = sysDepartmentMapper.selectAll();
            if (departments != null) {
                for (SysDepartment department : departments) {
                    if (department != null && department.getId() != null && department.getName() != null
                            && departmentIds.contains(department.getId())) {
                        nameMap.put(department.getId(), department.getName());
                    }
                }
            }
            return nameMap;
        }
        if (receiveType == NotificationReceiverType.CUSTOM) {
            List<SysSchoolDepartment> departments = schoolDepartmentMapper.selectAll(2);
            if (departments != null) {
                for (SysSchoolDepartment department : departments) {
                    if (department != null && department.getId() != null && department.getName() != null
                            && departmentIds.contains(department.getId())) {
                        nameMap.put(department.getId(), department.getName());
                    }
                }
            }
        }
        return nameMap;
    }

    private List<String> resolveTargetNames(List<NotificationReceiverTarget> targets, Map<String, String> nameMap) {
        if (targets == null || targets.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>(targets.size());
        for (NotificationReceiverTarget target : targets) {
            String name = lookupReceiveName(nameMap, target);
            if (StringUtils.hasText(name)) {
                names.add(name);
            }
        }
        return names;
    }

    private String lookupReceiveName(Map<String, String> nameMap, NotificationReceiverTarget target) {
        if (!StringUtils.hasText(target.getParentUserId())) {
            return null;
        }
        if (target.getDepartmentId() != null) {
            String name = nameMap.get(parentDeptKey(target.getParentUserId(), target.getDepartmentId()));
            if (StringUtils.hasText(name)) {
                return name;
            }
        }
        return nameMap.get(target.getParentUserId());
    }

    private Map<String, String> buildReceiveNameMap(List<NotificationReceiverTarget> targets,
                                                    NotificationReceiverType receiveType) {
        List<String> uniqueIds = distinctNonBlankIds(targets.stream()
                .map(NotificationReceiverTarget::getParentUserId)
                .collect(Collectors.toList()));
        if (uniqueIds.isEmpty()) {
            return Collections.emptyMap();
        }
        if (receiveType == NotificationReceiverType.WECOM) {
            return buildWecomNameMap(uniqueIds);
        }
        if (receiveType == NotificationReceiverType.CUSTOM) {
            return buildCustomNameMap(uniqueIds);
        }
        return Collections.emptyMap();
    }

    private Map<String, String> buildWecomNameMap(List<String> parentUserIds) {
        List<SysSchoolFamilyContact> contacts = schoolFamilyContactMapper.selectByParentUserIds(parentUserIds);
        if (contacts == null || contacts.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> nameMap = new HashMap<>();
        for (SysSchoolFamilyContact contact : contacts) {
            if (!StringUtils.hasText(contact.getParentUserId())) {
                continue;
            }
            String displayName = buildWecomDisplayName(contact);
            if (contact.getDepartmentId() != null) {
                nameMap.put(parentDeptKey(contact.getParentUserId(), contact.getDepartmentId()), displayName);
            }
            nameMap.putIfAbsent(contact.getParentUserId(), displayName);
        }
        return nameMap;
    }

    private Map<String, String> buildCustomNameMap(List<String> userids) {
        List<SysSchoolDepartmentMember> members = schoolDepartmentMemberMapper.selectMembersByUserids(userids);
        if (members == null || members.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> nameMap = new HashMap<>();
        for (SysSchoolDepartmentMember member : members) {
            if (!StringUtils.hasText(member.getUserid()) || !StringUtils.hasText(member.getName())) {
                continue;
            }
            if (member.getDepartmentId() != null) {
                nameMap.put(parentDeptKey(member.getUserid(), member.getDepartmentId()), member.getName());
            }
            nameMap.putIfAbsent(member.getUserid(), member.getName());
        }
        return nameMap;
    }

    private String buildWecomDisplayName(SysSchoolFamilyContact contact) {
        String studentName = StringUtils.hasText(contact.getStudentName()) ? contact.getStudentName() : "未知";
        String relationDesc = contact.getRelationDesc();
        if (StringUtils.hasText(relationDesc)) {
            return studentName + "-" + relationDesc;
        }
        return studentName;
    }

    private List<String> distinctNonBlankIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }
}
