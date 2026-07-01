package com.sms.system.service.impl;

import com.sms.system.entity.ClassSection;
import com.sms.system.entity.SysDepartment;
import com.sms.system.entity.SysSchoolFamilyContact;
import com.sms.system.entity.vo.ParentStudentMessageInfo;
import com.sms.system.mapper.SysDepartmentMapper;
import com.sms.system.service.IClassSectionService;
import com.sms.system.service.INotificationMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
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
    public List<ParentStudentMessageInfo> buildMessageInfos(List<SysSchoolFamilyContact> relations) {
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("開始構建消息信息，relations 數量: {}", relations.size());

        Map<Long, String> departmentNameMap = preloadDepartmentNames(relations);
        Map<String, ClassSection> classSectionMap = preloadClassSections(departmentNameMap);

        List<ParentStudentMessageInfo> messageInfos = new ArrayList<>();
        for (SysSchoolFamilyContact relation : relations) {
            String parentUserId = relation.getParentUserId();
            String studentUserId = relation.getStudentUserId();
            Long departmentId = relation.getDepartmentId();

            if (parentUserId == null || studentUserId == null) {
                log.warn("跳過無效的關係: parentUserId={}, studentUserId={}", parentUserId, studentUserId);
                continue;
            }

            String className = null;
            if (departmentId != null && departmentNameMap.containsKey(departmentId)) {
                String dsedjName = departmentNameMap.get(departmentId);
                if (dsedjName != null && classSectionMap.containsKey(dsedjName)) {
                    className = classSectionMap.get(dsedjName).getClassSectionSp();
                }
            }

            String studentName = relation.getStudentName();
            messageInfos.add(new ParentStudentMessageInfo(parentUserId, studentUserId, className, studentName));
        }

        log.info("消息信息構建完成，總數: {}", messageInfos.size());
        return messageInfos;
    }

    private Map<Long, String> preloadDepartmentNames(List<SysSchoolFamilyContact> relations) {
        Set<Long> departmentIds = relations.stream()
                .map(SysSchoolFamilyContact::getDepartmentId)
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
