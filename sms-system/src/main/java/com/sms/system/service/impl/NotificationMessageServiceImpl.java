package com.sms.system.service.impl;

import com.sms.system.entity.ClassSection;
import com.sms.system.entity.SysDepartment;
import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.entity.SysParentStudentRelation;
import com.sms.system.entity.vo.ParentStudentMessageInfo;
import com.sms.system.mapper.SysDepartmentMapper;
import com.sms.system.mapper.SysParentStudentRelationMapper;
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

    @Autowired
    private SysParentStudentRelationMapper parentStudentRelationMapper;

    /**
     * 根據家長-學生綁定關系列表，構建完整的消息信息
     *
     * @param bindings 家長-學生綁定關系列表
     * @return 家長-學生消息信息列表
     */
    @Override
    public List<ParentStudentMessageInfo> buildMessageInfos(List<SysDepartmentParentBinding> bindings) {
        // 如果綁定關系列表爲空，則返回空列表
        if (bindings == null || bindings.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("開始構建消息信息，bindings 數量: {}", bindings.size());

        // 1. 批量預加載部門名稱
        Map<Long, String> departmentNameMap = preloadDepartmentNames(bindings);
        log.info("預加載部門名稱完成，部門數量: {}", departmentNameMap.size());

        // 2. 批量預加載班級信息
        Map<String, ClassSection> classSectionMap = preloadClassSections(departmentNameMap);
        log.info("預加載班級信息完成，班級數量: {}", classSectionMap.size());

        // 3. 批量預加載學生姓名
        Map<String, String> studentNameMap = preloadStudentNames(bindings);
        log.info("預加載學生姓名完成，學生數量: {}", studentNameMap.size());

        // 4. 構建消息信息列表
        List<ParentStudentMessageInfo> messageInfos = new ArrayList<>();
        for (SysDepartmentParentBinding binding : bindings) {
            String parentUserId = binding.getParentUserId();
            String studentUserId = binding.getStudentUserId();
            Long departmentId = binding.getDepartmentId();

            if (parentUserId == null || studentUserId == null) {
                log.warn("跳過無效的綁定關係: parentUserId={}, studentUserId={}", parentUserId, studentUserId);
                continue;
            }

            // 獲取班級名稱
            String className = null;
            if (departmentId != null && departmentNameMap.containsKey(departmentId)) {
                String dsedjName = departmentNameMap.get(departmentId);
                log.debug("部門 ID: {}, 部門名稱: {}", departmentId, dsedjName);
                if (dsedjName != null && classSectionMap.containsKey(dsedjName)) {
                    ClassSection classSection = classSectionMap.get(dsedjName);
                    className = classSection.getClassSectionSp(); // 使用 SP 班級名
                    log.debug("找到班級: {} -> {}", dsedjName, className);
                } else {
                    log.warn("未找到匹配的班級信息，部門名稱: {}", dsedjName);
                }
            } else {
                log.warn("部門 ID 爲空或未找到部門名稱，departmentId: {}", departmentId);
            }

            // 獲取學生姓名
            String studentName = studentNameMap.get(studentUserId);
            if (studentName == null) {
                log.warn("未找到學生姓名，studentUserId: {}", studentUserId);
            }

            log.info("構建消息 - 家長: {}, 學生: {}, 班級: {}, 學生名: {}", 
                    parentUserId, studentUserId, className, studentName);
            messageInfos.add(new ParentStudentMessageInfo(parentUserId, studentUserId, className, studentName));
        }

        log.info("消息信息構建完成，總數: {}", messageInfos.size());
        return messageInfos;
    }

    /**
     * 批量預加載部門名稱
     *
     * @param bindings 家長-學生綁定關系列表
     * @return 部門 ID -> 部門名稱 的映射
     */
    private Map<Long, String> preloadDepartmentNames(List<SysDepartmentParentBinding> bindings) {
        // 提取所有唯一的部門 ID（過濾掉 null）
        Set<Long> departmentIds = bindings.stream()
                .map(SysDepartmentParentBinding::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (departmentIds.isEmpty()) {
            log.warn("所有綁定的 departmentId 都爲 null，無法預加載部門名稱");
            return Collections.emptyMap();
        }

        // 查詢所有部門信息
        List<SysDepartment> allDepartments = sysDepartmentMapper.selectAll();
        if (allDepartments == null || allDepartments.isEmpty()) {
            return Collections.emptyMap();
        }

        // 構建部門 ID -> 部門名稱的映射
        Map<Long, String> departmentNameMap = new HashMap<>();
        for (SysDepartment dept : allDepartments) {
            if (dept != null && dept.getId() != null && dept.getName() != null) {
                departmentNameMap.put(dept.getId(), dept.getName());
            }
        }

        return departmentNameMap;
    }

    /**
     * 批量預加載班級信息
     *
     * @param departmentNameMap 部門名稱映射
     * @return DSEDJ 名稱 -> ClassSection 的映射
     */
    private Map<String, ClassSection> preloadClassSections(Map<Long, String> departmentNameMap) {
        if (departmentNameMap == null || departmentNameMap.isEmpty()) {
            return Collections.emptyMap();
        }

        // 提取所有唯一的部門名稱（DSEDJ）
        Set<String> dsedjNames = departmentNameMap.values().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (dsedjNames.isEmpty()) {
            return Collections.emptyMap();
        }

        // 批量查詢班級信息
        Map<String, ClassSection> classSectionMap = new HashMap<>();
        for (String dsedjName : dsedjNames) {
            ClassSection classSection = classSectionService.getByDsedjName(dsedjName);
            if (classSection != null) {
                classSectionMap.put(dsedjName, classSection);
            }
        }
        return classSectionMap;
    }

    /**
     * 批量預加載學生姓名
     *
     * @param bindings 家長-學生綁定關系列表
     * @return 學生用戶 ID -> 學生姓名 的映射
     */
    private Map<String, String> preloadStudentNames(List<SysDepartmentParentBinding> bindings) {

        // 提取所有唯一的學生用戶 ID
        Set<String> studentUserIds = bindings.stream()
                .map(SysDepartmentParentBinding::getStudentUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (studentUserIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 通過 sys_parent_student_relation 表查詢學生姓名
        // 注意：當前 Mapper 沒有直接通過 student_user_id 查詢的方法
        // 所以我們先查詢所有相關的家長-學生關係，然後過濾
        Set<String> parentUserIds = bindings.stream()
                .map(SysDepartmentParentBinding::getParentUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (parentUserIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 批量查詢家長-學生關係
        List<SysParentStudentRelation> relations = parentStudentRelationMapper.selectByParentUserIds(
                new ArrayList<>(parentUserIds));

        if (relations == null || relations.isEmpty()) {
            return Collections.emptyMap();
        }

        // 構建學生用戶 ID -> 學生姓名的映射
        Map<String, String> studentNameMap = new HashMap<>();
        for (SysParentStudentRelation relation : relations) {
            if (relation != null && relation.getStudentUserId() != null && relation.getStudentName() != null) {
                studentNameMap.put(relation.getStudentUserId(), relation.getStudentName());
            }
        }

        return studentNameMap;
    }
}
