package com.sms.system.service.impl.notification;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.SysDepartment;
import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.entity.SysParentStudentRelation;
import com.sms.system.entity.SysSchoolDepartmentMember;
import com.sms.system.entity.notification.NotificationReceiver;
import com.sms.system.entity.vo.ResolvedReceiversVO;
import com.sms.system.mapper.SysDepartmentMapper;
import com.sms.system.mapper.SysDepartmentParentBindingMapper;
import com.sms.system.mapper.SysParentStudentRelationMapper;
import com.sms.system.mapper.SysSchoolDepartmentMemberMapper;
import com.sms.system.mapper.notification.NotificationReceiverMapper;
import com.sms.system.service.notification.INotificationReceiverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知接收对象 Service 业务层处理
 *
 */
@Service
public class NotificationReceiverServiceImpl implements INotificationReceiverService {

    private static final Logger log = LoggerFactory.getLogger(NotificationReceiverServiceImpl.class);

    /**
     * 接收類別常量：按部門發布
     */
    private static final String RECEIVE_TYPE_DEPARTMENT = "1";

    /**
     * 接收類別常量：按個人發布
     */
    private static final String RECEIVE_TYPE_PERSONAL = "2";

    /**
     * 對象類型常量：家長
     */
    private static final Integer TARGET_TYPE_PARENT = 1;

    /**
     * 對象類型常量：學生
     */
    private static final Integer TARGET_TYPE_STUDENT = 2;

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

    /**
     * 根据通知 ID 查询接收对象列表
     *
     * @param notificationId 通知 ID
     * @return 接收对象集合
     */
    @Override
    public List<NotificationReceiver> selectByNotificationId(Long notificationId) {
        return notificationReceiverMapper.selectByNotificationId(notificationId);
    }
    
    /**
     * 新增接收对象
     *
     * @param receiver 接收对象
     * @return 结果
     */
    @Override
    public int save(NotificationReceiver receiver) {
        return notificationReceiverMapper.insert(receiver);
    }

    /**
     * 解析接收者列表，將其轉換為企業微信可識別的 userid 集合
     *
     * @param receivers 原始通告接收者配置列表
     * @return 包含 to_parent_userid、to_student_userid、to_party 的 Map 集合
     */
    @Override
    public ResolvedReceiversVO resolveReceivers(List<NotificationReceiver> receivers) {
        Set<String> parentUserIds = new HashSet<>();
        Set<String> studentUserIds = new HashSet<>();
        Set<String> partyIds = new HashSet<>(); // 暫時未實現具體部門 ID 的解析，保留擴展性
        List<SysDepartmentParentBinding> bindings = new ArrayList<>();

        if (receivers == null || receivers.isEmpty()) {
            log.warn("notification receivers are empty");
            return buildResult(parentUserIds, studentUserIds, partyIds, bindings);
        }

        for (NotificationReceiver receiver : receivers) {
            String receiveType = receiver.getReceiveType();
            String receiveData = receiver.getReceiveData();

            if (receiveData == null || receiveData.trim().isEmpty()) {
                continue;
            }

            try {
                JSONArray dataArray = JSON.parseArray(receiveData);
                if (dataArray == null || dataArray.isEmpty()) {
                    continue;
                }

                // 遍歷所有數據項進行解析
                for (int i = 0; i < dataArray.size(); i++) {
                    JSONObject dataItem = dataArray.getJSONObject(i);
                    parseAndResolveDataItem(receiveType, dataItem, parentUserIds, studentUserIds, bindings);
                }
            } catch (Exception e) {
                log.error("failed to resolve notification receivers, data: {}", receiveData, e);
            }
        }

        return buildResult(parentUserIds, studentUserIds, partyIds, bindings);
    }

    /**
     * 解析單個接收者數據元素
     */
    private void parseAndResolveDataItem(String receiveType, JSONObject dataItem, Set<String> parentUserIds, Set<String> studentUserIds, List<SysDepartmentParentBinding> bindings) {
        Integer type = dataItem.getInteger("type");
        JSONArray ids = dataItem.getJSONArray("receive_ids");
        
        // 兼容不同的字段命名
        if (ids == null || ids.isEmpty()) {
            ids = dataItem.getJSONArray("ids");
        }

        // 校驗必填項
        if (type == null || ids == null || ids.isEmpty()) {
            return;
        }

        List<Long> idList = ids.toJavaList(Long.class);

        // 根據接收類型分發到不同的解析處理邏輯
        if (RECEIVE_TYPE_PERSONAL.equals(receiveType)) {
            resolvePersonalReceivers(type, idList, parentUserIds, studentUserIds, bindings);
        } else if (RECEIVE_TYPE_DEPARTMENT.equals(receiveType)) {
            resolveDepartmentReceivers(type, idList, parentUserIds, studentUserIds, bindings);
        }
    }

    /**
     * 處理個人維度的接收者（直接根據 ID 查詢）
     */
    private void resolvePersonalReceivers(Integer type, List<Long> idList, Set<String> parentUserIds, Set<String> studentUserIds, List<SysDepartmentParentBinding> bindings) {
        if (TARGET_TYPE_PARENT.equals(type)) {
            resolveParentUserIds(idList, parentUserIds, bindings);
        } else if (TARGET_TYPE_STUDENT.equals(type)) {
            resolveStudentUserIds(idList, studentUserIds);
        }
    }

    /**
     * 處理部門維度的接收者（根據部門 ID 查詢下屬成員）
     */
    private void resolveDepartmentReceivers(Integer type, List<Long> idList, Set<String> parentUserIds, Set<String> studentUserIds, List<SysDepartmentParentBinding> outBindings) {
        if (TARGET_TYPE_PARENT.equals(type)) {
            resolveParentUserIdsByDepartment(idList, parentUserIds, outBindings);
        } else if (TARGET_TYPE_STUDENT.equals(type)) {
            resolveStudentUserIdsByDepartment(idList, studentUserIds);
        }
    }

    /**
     * 根據家長學生關係 ID 列表，獲取對應的家長 UserID
     */
    private void resolveParentUserIds(List<Long> ids, Set<String> parentUserIds, List<SysDepartmentParentBinding> bindings) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<SysParentStudentRelation> relations = parentStudentRelationMapper.selectByIds(ids);
        if (relations != null) {
            for (SysParentStudentRelation relation : relations) {
                if (relation.getParentUserId() != null && !relation.getParentUserId().trim().isEmpty()) {
                    parentUserIds.add(relation.getParentUserId());
                    
                    // 提取精確的綁定關係，避免後續重複查庫導致包含未選擇的學生
                    SysDepartmentParentBinding binding = new SysDepartmentParentBinding();
                    binding.setParentUserId(relation.getParentUserId());
                    binding.setStudentUserId(relation.getStudentUserId());
                    bindings.add(binding);
                }
            }
        }
    }

    /**
     * 根據成員 ID 列表，獲取對應的學生 UserID
     */
    private void resolveStudentUserIds(List<Long> ids, Set<String> studentUserIds) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<SysSchoolDepartmentMember> members = schoolDepartmentMemberMapper.selectMembersByIds(ids);
        if (members != null) {
            for (SysSchoolDepartmentMember member : members) {
                if (member.getUserid() != null && !member.getUserid().trim().isEmpty()) {
                    studentUserIds.add(member.getUserid());
                }
            }
        }
    }

    /**
     * 根據部門 ID 列表，獲取部門下綁定的所有家長 UserID
     * 注意：如果傳入的是上層部門 ID（type > 1），需要遞歸找到所有 type=1 的班級部門 ID
     */
    private void resolveParentUserIdsByDepartment(List<Long> departmentIds, Set<String> parentUserIds, List<SysDepartmentParentBinding> outBindings) {
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
                    outBindings.add(binding);
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
     * 根據部門 ID 列表，獲取該部門下所有的學生 UserID
     */
    private void resolveStudentUserIdsByDepartment(List<Long> departmentIds, Set<String> studentUserIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return;
        }

        List<SysSchoolDepartmentMember> members = schoolDepartmentMemberMapper.selectMembersByDepartmentIds(departmentIds);
        if (members != null) {
            for (SysSchoolDepartmentMember member : members) {
                if (member.getUserid() != null && !member.getUserid().trim().isEmpty()) {
                    studentUserIds.add(member.getUserid());
                }
            }
        }
    }

    /**
     * 封裝並構建最終的返回結果
     */
    private ResolvedReceiversVO buildResult(Set<String> parentUserIds, Set<String> studentUserIds, Set<String> partyIds, List<SysDepartmentParentBinding> bindings) {
        return new ResolvedReceiversVO(
                new ArrayList<>(parentUserIds),
                new ArrayList<>(studentUserIds),
                new ArrayList<>(partyIds),
                bindings
        );
    }
}
