package com.sms.system.service.impl.notification;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.entity.SysParentStudentRelation;
import com.sms.system.entity.SysSchoolDepartmentMember;
import com.sms.system.entity.notification.NotificationReceiver;
import com.sms.system.mapper.SysDepartmentParentBindingMapper;
import com.sms.system.mapper.SysParentStudentRelationMapper;
import com.sms.system.mapper.SysSchoolDepartmentMemberMapper;
import com.sms.system.mapper.notification.NotificationReceiverMapper;
import com.sms.system.service.notification.INotificationReceiverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public Map<String, List<String>> resolveReceivers(List<NotificationReceiver> receivers) {
        Set<String> parentUserIds = new HashSet<>();
        Set<String> studentUserIds = new HashSet<>();
        Set<String> partyIds = new HashSet<>(); // 暫時未實現具體部門 ID 的解析，保留擴展性

        if (receivers == null || receivers.isEmpty()) {
            log.warn("notification receivers are empty");
            return buildResult(parentUserIds, studentUserIds, partyIds);
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
                    parseAndResolveDataItem(receiveType, dataItem, parentUserIds, studentUserIds);
                }
            } catch (Exception e) {
                log.error("failed to resolve notification receivers, data: {}", receiveData, e);
            }
        }

        return buildResult(parentUserIds, studentUserIds, partyIds);
    }

    /**
     * 解析單個接收者數據元素
     */
    private void parseAndResolveDataItem(String receiveType, JSONObject dataItem, Set<String> parentUserIds, Set<String> studentUserIds) {
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
            resolvePersonalReceivers(type, idList, parentUserIds, studentUserIds);
        } else if (RECEIVE_TYPE_DEPARTMENT.equals(receiveType)) {
            resolveDepartmentReceivers(type, idList, parentUserIds, studentUserIds);
        }
    }

    /**
     * 處理個人維度的接收者（直接根據 ID 查詢）
     */
    private void resolvePersonalReceivers(Integer type, List<Long> idList, Set<String> parentUserIds, Set<String> studentUserIds) {
        if (TARGET_TYPE_PARENT.equals(type)) {
            resolveParentUserIds(idList, parentUserIds);
        } else if (TARGET_TYPE_STUDENT.equals(type)) {
            resolveStudentUserIds(idList, studentUserIds);
        }
    }

    /**
     * 處理部門維度的接收者（根據部門 ID 查詢下屬成員）
     */
    private void resolveDepartmentReceivers(Integer type, List<Long> idList, Set<String> parentUserIds, Set<String> studentUserIds) {
        if (TARGET_TYPE_PARENT.equals(type)) {
            resolveParentUserIdsByDepartment(idList, parentUserIds);
        } else if (TARGET_TYPE_STUDENT.equals(type)) {
            resolveStudentUserIdsByDepartment(idList, studentUserIds);
        }
    }

    /**
     * 根據家長學生關係 ID 列表，獲取對應的家長 UserID
     */
    private void resolveParentUserIds(List<Long> ids, Set<String> parentUserIds) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<SysParentStudentRelation> relations = parentStudentRelationMapper.selectByIds(ids);
        if (relations != null) {
            for (SysParentStudentRelation relation : relations) {
                if (relation.getParentUserId() != null && !relation.getParentUserId().trim().isEmpty()) {
                    parentUserIds.add(relation.getParentUserId());
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
     */
    private void resolveParentUserIdsByDepartment(List<Long> departmentIds, Set<String> parentUserIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return;
        }

        List<SysDepartmentParentBinding> bindings = departmentParentBindingMapper.selectByDepartmentIds(departmentIds);
        if (bindings != null) {
            for (SysDepartmentParentBinding binding : bindings) {
                if (binding.getParentUserId() != null && !binding.getParentUserId().trim().isEmpty()) {
                    parentUserIds.add(binding.getParentUserId());
                }
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
    private Map<String, List<String>> buildResult(Set<String> parentUserIds, Set<String> studentUserIds, Set<String> partyIds) {
        Map<String, List<String>> result = new HashMap<>();
        result.put("to_parent_userid", new ArrayList<>(parentUserIds));
        result.put("to_student_userid", new ArrayList<>(studentUserIds));
        result.put("to_party", new ArrayList<>(partyIds));
        return result;
    }
}
