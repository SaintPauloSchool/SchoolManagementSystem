package com.sms.system.service.impl.notification;

import com.sms.common.exception.ServiceException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.SysDepartment;
import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.entity.SysParentStudentRelation;
import com.sms.system.entity.SysSchoolDepartmentMember;
import com.sms.system.entity.dto.NotificationReceiverSaveDTO;
import com.sms.system.entity.notification.NotificationReceiver;
import com.sms.system.entity.vo.NotificationReceiverVO;
import com.sms.system.entity.vo.ResolvedReceiversVO;
import com.sms.system.mapper.SysDepartmentMapper;
import com.sms.system.mapper.SysDepartmentParentBindingMapper;
import com.sms.system.mapper.SysParentStudentRelationMapper;
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

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * 通知接收對象 Service 業務層處理
 *
 */
@Service
public class NotificationReceiverServiceImpl implements INotificationReceiverService {

    private static final Logger log = LoggerFactory.getLogger(NotificationReceiverServiceImpl.class);

    /**
     * 接收類別常量：按部門發佈
     */
    private static final String RECEIVE_TYPE_DEPARTMENT = "1";

    /**
     * 接收類別常量：按個人發佈
     */
    private static final String RECEIVE_TYPE_PERSONAL = "2";

    /**
     * 對象類型常量：企業微信數據
     */
    private static final Integer TARGET_TYPE_WECOM = 1;

    /**
     * 對象類型常量：自定義數據
     */
    private static final Integer TARGET_TYPE_CUSTOM = 2;

    @Autowired
    private NotificationReceiverMapper notificationReceiverMapper;

    @Autowired
    private SysParentStudentRelationMapper parentStudentRelationMapper;

    @Autowired
    private SysSchoolDepartmentMemberMapper schoolDepartmentMemberMapper;

    @Autowired
    private SysDepartmentParentBindingMapper departmentParentBindingMapper;

    @Autowired
    private SysDepartmentMapper sysDepartmentMapper;

    @Autowired
    private SysSchoolDepartmentServiceImpl sysSchoolDepartmentService;

    /**
     * 根據通知 ID 查詢接收對象列表
     *
     * @param notificationId 通知 ID
     * @return 接收對象集合
     */
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
        Set<String> parentUserIds = new HashSet<>();
        Set<String> studentUserIds = new HashSet<>();
        Set<String> partyIds = new HashSet<>(); // 暫時未實現具體部門 ID 的解析，保留擴展性
        List<SysDepartmentParentBinding> bindings = new ArrayList<>();
        Map<String, Long> studentDepartmentIds = new HashMap<>();
        Set<String> bindingKeys = new HashSet<>();

        if (receivers == null || receivers.isEmpty()) {
            log.warn("notification receivers are empty");
            return buildResult(parentUserIds, studentUserIds, partyIds, bindings, studentDepartmentIds);
        }

        try {
            for (NotificationReceiver receiver : receivers) {
                String receiveType = receiver.getReceiveType();
                String receiveData = receiver.getReceiveData();

                if (receiveData == null || receiveData.trim().isEmpty()) {
                    continue;
                }

                JSONArray dataArray = JSON.parseArray(receiveData);
                if (dataArray == null || dataArray.isEmpty()) {
                    continue;
                }

                for (int i = 0; i < dataArray.size(); i++) {
                    JSONObject dataItem = dataArray.getJSONObject(i);
                    parseAndResolveDataItem(receiveType, dataItem, parentUserIds, studentUserIds, bindings,
                            bindingKeys, studentDepartmentIds, strictDepartmentCheck);
                }
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("failed to resolve notification receivers", e);
            throw new ServiceException("解析接收對象失敗：" + e.getMessage());
        }

        // 最終去重：以 parentUserId + studentUserId + departmentId 為唯一鍵
        List<SysDepartmentParentBinding> uniqueBindings = new ArrayList<>();
        Set<String> finalBindingKeys = new HashSet<>();
        for (SysDepartmentParentBinding binding : bindings) {
            if (binding.getParentUserId() != null && binding.getStudentUserId() != null) {
                String key = buildBindingKey(binding.getParentUserId(), binding.getStudentUserId(), binding.getDepartmentId());
                if (!finalBindingKeys.contains(key)) {
                    uniqueBindings.add(binding);
                    finalBindingKeys.add(key);
                } else {
                    log.warn("檢測到重複的綁定關係，已移除: parentUserId={}, studentUserId={}, departmentId={}",
                            binding.getParentUserId(), binding.getStudentUserId(), binding.getDepartmentId());
                }
            }
        }

        log.info("解析完成 - parentUserIds: {}, studentUserIds: {}, 綁定關係數: {} (去重前: {})",
                parentUserIds.size(), studentUserIds.size(), uniqueBindings.size(), bindings.size());

        return buildResult(parentUserIds, studentUserIds, partyIds, uniqueBindings, studentDepartmentIds);
    }

    /**
     * 解析單個接收者數據元素
     */
    private void parseAndResolveDataItem(String receiveType, JSONObject dataItem, Set<String> parentUserIds,
                                         Set<String> studentUserIds, List<SysDepartmentParentBinding> bindings,
                                         Set<String> bindingKeys, Map<String, Long> studentDepartmentIds,
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
            resolvePersonalReceivers(type, idList, departmentIdList, parentUserIds, studentUserIds, bindings,
                    bindingKeys, studentDepartmentIds, strictDepartmentCheck);
        } else if (RECEIVE_TYPE_DEPARTMENT.equals(receiveType)) {
            resolveDepartmentReceivers(type, idList, parentUserIds, studentUserIds, bindings, bindingKeys,
                    studentDepartmentIds);
        }
    }

    /**
     * 從 receive_data 中解析與 receive_ids 對齊的 department_ids 列表
     */
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

    /**
     * 處理個人維度的接收者（直接根據 ID 查詢）
     */
    private void resolvePersonalReceivers(Integer type, List<Long> idList, List<Long> departmentIdList,
                                          Set<String> parentUserIds, Set<String> studentUserIds,
                                          List<SysDepartmentParentBinding> bindings, Set<String> bindingKeys,
                                          Map<String, Long> studentDepartmentIds, boolean strictDepartmentCheck) {
        if (TARGET_TYPE_WECOM.equals(type)) {
            resolveParentUserIds(idList, departmentIdList, parentUserIds, bindings, bindingKeys, strictDepartmentCheck);
        } else if (TARGET_TYPE_CUSTOM.equals(type)) {
            resolveCustomMemberParentUserIds(idList, departmentIdList, parentUserIds, studentDepartmentIds);
        }
    }

    /**
     * 處理部門維度的接收者（根據部門 ID 查詢下屬成員）
     */
    private void resolveDepartmentReceivers(Integer type, List<Long> idList, Set<String> parentUserIds,
                                            Set<String> studentUserIds, List<SysDepartmentParentBinding> outBindings,
                                            Set<String> bindingKeys, Map<String, Long> studentDepartmentIds) {
        if (TARGET_TYPE_WECOM.equals(type)) {
            resolveParentUserIdsByDepartment(idList, parentUserIds, outBindings, bindingKeys);
        } else if (TARGET_TYPE_CUSTOM.equals(type)) {
            resolveCustomMemberParentUserIdsByDepartment(idList, parentUserIds, studentDepartmentIds);
        }
    }

    /**
     * 根據家長學生關係 ID 列表，獲取對應的家長 UserID
     */
    private void resolveParentUserIds(List<Long> ids, List<Long> departmentIds, Set<String> parentUserIds,
                                      List<SysDepartmentParentBinding> bindings, Set<String> bindingKeys,
                                      boolean strictDepartmentCheck) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Long> uniqueRelationIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (uniqueRelationIds.isEmpty()) {
            return;
        }

        List<SysParentStudentRelation> relations = parentStudentRelationMapper.selectByIds(uniqueRelationIds);
        if (relations == null || relations.isEmpty()) {
            return;
        }

        Map<Long, SysParentStudentRelation> relationMap = relations.stream()
                .filter(relation -> relation.getId() != null)
                .collect(Collectors.toMap(SysParentStudentRelation::getId, relation -> relation, (a, b) -> a));

        List<String> studentUserIds = relations.stream()
                .map(SysParentStudentRelation::getStudentUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<String, List<SysDepartmentParentBinding>> parentStudentBindingsMap = new HashMap<>();
        if (!studentUserIds.isEmpty()) {
            List<SysDepartmentParentBinding> existingBindings = departmentParentBindingMapper.selectByStudentUserIds(studentUserIds);
            if (existingBindings != null) {
                for (SysDepartmentParentBinding binding : existingBindings) {
                    if (binding.getStudentUserId() != null && binding.getParentUserId() != null) {
                        String key = binding.getParentUserId() + "_" + binding.getStudentUserId();
                        parentStudentBindingsMap.computeIfAbsent(key, k -> new ArrayList<>()).add(binding);
                    }
                }
            }
        }

        for (int i = 0; i < ids.size(); i++) {
            Long relationId = ids.get(i);
            if (relationId == null) {
                continue;
            }
            SysParentStudentRelation relation = relationMap.get(relationId);
            if (relation == null || relation.getParentUserId() == null || relation.getParentUserId().trim().isEmpty()) {
                continue;
            }
            parentUserIds.add(relation.getParentUserId());

            String studentUserId = relation.getStudentUserId();
            String relationKey = relation.getParentUserId() + "_" + studentUserId;
            Long explicitDepartmentId = i < departmentIds.size() ? departmentIds.get(i) : null;
            SysDepartmentParentBinding resolvedBinding = resolveParentBinding(
                    relation, explicitDepartmentId, parentStudentBindingsMap.get(relationKey), strictDepartmentCheck);
            Long departmentId = resolvedBinding != null ? resolvedBinding.getDepartmentId() : null;

            String uniqueKey = buildBindingKey(relation.getParentUserId(), studentUserId, departmentId);
            if (bindingKeys.contains(uniqueKey)) {
                log.debug("跳過重複的綁定關係: parentUserId={}, studentUserId={}, departmentId={}",
                        relation.getParentUserId(), studentUserId, departmentId);
                continue;
            }

            if (resolvedBinding != null) {
                bindings.add(resolvedBinding);
            } else {
                SysDepartmentParentBinding binding = new SysDepartmentParentBinding();
                binding.setParentUserId(relation.getParentUserId());
                binding.setStudentUserId(studentUserId);
                binding.setDepartmentId(null);
                bindings.add(binding);
            }

            bindingKeys.add(uniqueKey);
        }
    }

    /**
     * 解析家長綁定：優先使用前端傳入的 department_id，其次唯一匹配，否則返回 null
     */
    private SysDepartmentParentBinding resolveParentBinding(SysParentStudentRelation relation, Long explicitDepartmentId,
                                                            List<SysDepartmentParentBinding> candidateBindings,
                                                            boolean strictDepartmentCheck) {
        if (explicitDepartmentId != null) {
            if (candidateBindings != null) {
                for (SysDepartmentParentBinding binding : candidateBindings) {
                    if (explicitDepartmentId.equals(binding.getDepartmentId())) {
                        return binding;
                    }
                }
            }
            SysDepartmentParentBinding binding = new SysDepartmentParentBinding();
            binding.setParentUserId(relation.getParentUserId());
            binding.setStudentUserId(relation.getStudentUserId());
            binding.setDepartmentId(explicitDepartmentId);
            return binding;
        }
        if (candidateBindings == null || candidateBindings.isEmpty()) {
            return null;
        }
        if (candidateBindings.size() == 1) {
            return candidateBindings.get(0);
        }
        if (!strictDepartmentCheck) {
            log.warn("家長學生關係存在多個部門綁定且未指定 department_id，已使用第一筆: parentUserId={}, studentUserId={}",
                    relation.getParentUserId(), relation.getStudentUserId());
            return candidateBindings.get(0);
        }
        throw new ServiceException(String.format(
                "家長「%s」存在多個班級綁定，請在選人時指定班級後再發送（relationId=%d）",
                relation.getStudentName() != null ? relation.getStudentName() : relation.getParentUserId(),
                relation.getId()));
    }

    /**
     * 根據自定義家校通訊錄成員 ID 列表，獲取對應的家長 UserID
     * <p>sys_school_department_member.userid 存的是企微家長 ID，應走 to_parent_userid 發送。</p>
     */
    private void resolveCustomMemberParentUserIds(List<Long> ids, List<Long> departmentIds, Set<String> parentUserIds,
                                                  Map<String, Long> studentDepartmentIds) {
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
            parentUserIds.add(member.getUserid());
            Long departmentId = i < departmentIds.size() ? departmentIds.get(i) : null;
            if (departmentId == null) {
                departmentId = member.getDepartmentId();
            }
            if (departmentId != null) {
                studentDepartmentIds.put(member.getUserid(), departmentId);
            }
        }
    }

    /**
     * 根據部門 ID 列表，獲取部門下綁定的所有家長 UserID
     * 注意：如果傳入的是上層部門 ID（type > 1），需要遞歸找到所有 type=1 的班級部門 ID
     */
    private void resolveParentUserIdsByDepartment(List<Long> departmentIds, Set<String> parentUserIds, List<SysDepartmentParentBinding> outBindings, Set<String> bindingKeys) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return;
        }

        // 1. 遞歸獲取所有 type=1 的班級部門 ID
        List<Long> classDepartmentIds = resolveClassDepartmentIds(departmentIds);
        
        log.info("解析部門家長綁定 - 輸入部門 IDs: {}, 解析後班級部門 IDs: {}", departmentIds, classDepartmentIds);
        
        if (classDepartmentIds.isEmpty()) {
            return;
        }

        // 2. 根據班級部門 ID 查詢家長綁定關係
        List<SysDepartmentParentBinding> bindings = departmentParentBindingMapper.selectByDepartmentIds(classDepartmentIds);
        if (bindings != null) {
            for (SysDepartmentParentBinding binding : bindings) {
                if (binding.getParentUserId() != null && !binding.getParentUserId().trim().isEmpty()) {
                    parentUserIds.add(binding.getParentUserId());
                    
                    // 去重檢查：以 parentUserId + studentUserId + departmentId 為唯一鍵
                    String bindingKey = buildBindingKey(binding.getParentUserId(), binding.getStudentUserId(), binding.getDepartmentId());
                    if (bindingKeys.contains(bindingKey)) {
                        log.debug("跳過重複的部門綁定關係: parentUserId={}, studentUserId={}, departmentId={}",
                                binding.getParentUserId(), binding.getStudentUserId(), binding.getDepartmentId());
                        continue;
                    }
                    
                    outBindings.add(binding);
                    bindingKeys.add(bindingKey);
                }
            }
        }
    }

    /**
     * 遞歸獲取所有 type=1 的班級部門 ID
     * 如果傳入的部門 ID 是上層部門（type > 1），則找到其下所有 type=1 的子部門
     *
     * @param departmentIds 部門 ID 列表
     * @return 所有 type=1 的班級部門 ID 列表
     */
    private List<Long> resolveClassDepartmentIds(List<Long> departmentIds) {
        // 如果傳入的部門 ID 列表為空，則返回空列表
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 查詢所有部門信息
        List<SysDepartment> allDepartments = sysDepartmentMapper.selectAll();
        if (allDepartments == null || allDepartments.isEmpty()) {
            return departmentIds;
        }

        // 2. 構建部門 ID 到部門對象的映射
        Map<Long, SysDepartment> deptMap = allDepartments.stream()
                .filter(Objects::nonNull)
                .filter(dept -> dept.getId() != null)
                .collect(Collectors.toMap(SysDepartment::getId, dept -> dept, (a, b) -> a));

        // 3. 對每個傳入的部門 ID，遞歸查找其下所有 type=1 的子部門
        Set<Long> classDepartmentIds = new HashSet<>();
        for (Long deptId : departmentIds) {
            SysDepartment dept = deptMap.get(deptId);
            if (dept == null) {
                continue;
            }

            // 如果已經是 type=1，直接添加
            if (Integer.valueOf(1).equals(dept.getType())) {
                classDepartmentIds.add(deptId);
            } else {
                // 否則遞歸查找所有 type=1 的子部門
                collectClassDepartmentIds(deptId, allDepartments, classDepartmentIds);
            }
        }

        return new ArrayList<>(classDepartmentIds);
    }

    /**
     * 遞歸收集某個部門下所有 type=1 的班級部門 ID
     *
     * @param parentId 父部門 ID
     * @param allDepartments 所有部門列表
     * @param classDepartmentIds 收集結果的集合
     */
    private void collectClassDepartmentIds(Long parentId, List<SysDepartment> allDepartments, Set<Long> classDepartmentIds) {
        // 如果輸入參數為空，則返回
        if (parentId == null || allDepartments == null) {
            return;
        }

        // 找到所有直接子部門
        List<SysDepartment> children = allDepartments.stream()
                .filter(Objects::nonNull)
                .filter(dept -> dept.getParentId() != null)
                .filter(dept -> dept.getParentId().longValue() == parentId)
                .collect(Collectors.toList());

        for (SysDepartment child : children) {
            if (child.getId() == null) {
                continue;
            }

            // 如果是 type=1 的班級部門，添加到結果集
            if (Integer.valueOf(1).equals(child.getType())) {
                classDepartmentIds.add(child.getId());
            } else {
                // 否則繼續遞歸查找
                collectClassDepartmentIds(child.getId(), allDepartments, classDepartmentIds);
            }
        }
    }

    /**
     * 根據自定義家校通訊錄部門 ID 列表，獲取部門下所有成員的家長 UserID
     */
    private void resolveCustomMemberParentUserIdsByDepartment(List<Long> departmentIds, Set<String> parentUserIds,
                                                            Map<String, Long> studentDepartmentIds) {
        // 如果傳入的部門 ID 列表為空，則返回
        if (departmentIds == null || departmentIds.isEmpty()) {
            return;
        }

        // 1. 遞歸獲取所有子孫部門 ID（包括傳入的部門本身）
        List<Long> allDescendantDepartmentIds = sysSchoolDepartmentService.resolveAllDescendantDepartmentIdsByType(departmentIds, 2);
        
        log.info("解析自定義家校部門成員 - 輸入部門 IDs: {}, 解析後所有子孫部門 IDs: {}", departmentIds, allDescendantDepartmentIds);
        
        if (allDescendantDepartmentIds.isEmpty()) {
            return;
        }

        // 2. 根據所有子孫部門 ID 查詢成員
        List<SysSchoolDepartmentMember> members = schoolDepartmentMemberMapper.selectMembersByDepartmentIds(allDescendantDepartmentIds);
        if (members != null) {
            for (SysSchoolDepartmentMember member : members) {
                if (member.getUserid() == null || member.getUserid().trim().isEmpty()) {
                    continue;
                }
                parentUserIds.add(member.getUserid());
                if (member.getDepartmentId() != null) {
                    studentDepartmentIds.put(member.getUserid(), member.getDepartmentId());
                }
            }
        }
    }

    /**
     * 封裝並構建最終的返回結果
     */
    private ResolvedReceiversVO buildResult(Set<String> parentUserIds, Set<String> studentUserIds, Set<String> partyIds,
                                            List<SysDepartmentParentBinding> bindings,
                                            Map<String, Long> studentDepartmentIds) {
        return new ResolvedReceiversVO(
                new ArrayList<>(parentUserIds),
                new ArrayList<>(studentUserIds),
                new ArrayList<>(partyIds),
                bindings,
                studentDepartmentIds
        );
    }

    /**
     * 構建綁定關係唯一鍵（parent + student + department）
     */
    private String buildBindingKey(String parentUserId, String studentUserId, Long departmentId) {
        return parentUserId + "_" + studentUserId + "_" + (departmentId != null ? departmentId : "null");
    }
}
