package com.sms.system.service.impl;

import com.sms.system.entity.ClassSection;
import com.sms.system.entity.SysDepartment;
import com.sms.system.entity.SysSchoolFamilyContact;
import com.sms.system.entity.notification.receiver.NotificationReceiverTarget;
import com.sms.system.entity.vo.ParentStudentMessageInfo;
import com.sms.system.mapper.SysDepartmentMapper;
import com.sms.system.service.IClassSectionService;
import com.sms.system.service.INotificationMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知消息構建 Service 業務層處理
 */
@Service
public class NotificationMessageServiceImpl implements INotificationMessageService {

    private static final Logger log = LoggerFactory.getLogger(NotificationMessageServiceImpl.class);

    @Autowired
    private SysDepartmentMapper sysDepartmentMapper;

    @Autowired
    private IClassSectionService classSectionService;

    @Override
    public List<ParentStudentMessageInfo> buildMessageInfos(List<NotificationReceiverTarget> targets,
                                                            List<SysSchoolFamilyContact> relations) {
        if (targets == null || targets.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("開始構建消息資訊，targets 數量: {}", targets.size());

        Map<String, SysSchoolFamilyContact> relationByParentDept = indexRelationsByParentDept(relations);
        Map<Long, String> departmentNameMap = preloadDepartmentNames(targets);
        Map<String, ClassSection> classSectionMap = preloadClassSections(departmentNameMap);

        List<ParentStudentMessageInfo> messageInfos = new ArrayList<>();
        for (NotificationReceiverTarget target : targets) {
            String parentUserId = target.getParentUserId();
            String studentId = target.getStudentId();
            Long departmentId = target.getDepartmentId();

            if (!StringUtils.hasText(parentUserId) || !StringUtils.hasText(studentId)) {
                log.warn("跳過無效的接收目標: parentUserId={}, studentId={}", parentUserId, studentId);
                continue;
            }

            String className = null;
            if (departmentId != null && departmentNameMap.containsKey(departmentId)) {
                String dsedjName = departmentNameMap.get(departmentId);
                if (dsedjName != null && classSectionMap.containsKey(dsedjName)) {
                    className = classSectionMap.get(dsedjName).getClassSectionSp();
                }
            }

            String studentName = null;
            if (departmentId != null) {
                SysSchoolFamilyContact relation = relationByParentDept.get(parentDeptKey(parentUserId, departmentId));
                if (relation != null) {
                    studentName = relation.getStudentName();
                }
            }

            messageInfos.add(new ParentStudentMessageInfo(
                    parentUserId.trim(), studentId.trim(), className, studentName));
        }

        log.info("消息資訊構建完成，總數: {}", messageInfos.size());
        return messageInfos;
    }

    private Map<String, SysSchoolFamilyContact> indexRelationsByParentDept(List<SysSchoolFamilyContact> relations) {
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, SysSchoolFamilyContact> map = new HashMap<>();
        for (SysSchoolFamilyContact relation : relations) {
            if (!StringUtils.hasText(relation.getParentUserId()) || relation.getDepartmentId() == null) {
                continue;
            }
            map.put(parentDeptKey(relation.getParentUserId(), relation.getDepartmentId()), relation);
        }
        return map;
    }

    private String parentDeptKey(String parentUserId, Long departmentId) {
        return parentUserId + "_" + departmentId;
    }

    private Map<Long, String> preloadDepartmentNames(List<NotificationReceiverTarget> targets) {
        Set<Long> departmentIds = targets.stream()
                .map(NotificationReceiverTarget::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (departmentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<SysDepartment> allDepartments = sysDepartmentMapper.selectAll();
        if (allDepartments == null || allDepartments.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, String> departmentNameMap = new HashMap<>();
        for (SysDepartment dept : allDepartments) {
            if (dept != null && dept.getId() != null && dept.getName() != null
                    && departmentIds.contains(dept.getId())) {
                departmentNameMap.put(dept.getId(), dept.getName());
            }
        }
        return departmentNameMap;
    }

    private Map<String, ClassSection> preloadClassSections(Map<Long, String> departmentNameMap) {
        if (departmentNameMap == null || departmentNameMap.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> dsedjNames = departmentNameMap.values().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (dsedjNames.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, ClassSection> classSectionMap = new HashMap<>();
        for (String dsedjName : dsedjNames) {
            ClassSection classSection = classSectionService.getByDsedjName(dsedjName);
            if (classSection != null) {
                classSectionMap.put(dsedjName, classSection);
            }
        }
        return classSectionMap;
    }
}
