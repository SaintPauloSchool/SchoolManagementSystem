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
 * 通知消息构建 Service 业务层处理
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
     * 根据家长-学生绑定关系列表，构建完整的消息信息
     *
     * @param bindings 家长-学生绑定关系列表
     * @return 家长-学生消息信息列表
     */
    @Override
    public List<ParentStudentMessageInfo> buildMessageInfos(List<SysDepartmentParentBinding> bindings) {
        // 如果绑定关系列表为空，则返回空列表
        if (bindings == null || bindings.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("开始构建消息信息，bindings 数量: {}", bindings.size());

        // 1. 批量预加载部门名称
        Map<Long, String> departmentNameMap = preloadDepartmentNames(bindings);
        log.info("预加载部门名称完成，部门数量: {}", departmentNameMap.size());

        // 2. 批量预加载班级信息
        Map<String, ClassSection> classSectionMap = preloadClassSections(departmentNameMap);
        log.info("预加载班级信息完成，班级数量: {}", classSectionMap.size());

        // 3. 批量预加载学生姓名
        Map<String, String> studentNameMap = preloadStudentNames(bindings);
        log.info("预加载学生姓名完成，学生数量: {}", studentNameMap.size());

        // 4. 构建消息信息列表
        List<ParentStudentMessageInfo> messageInfos = new ArrayList<>();
        for (SysDepartmentParentBinding binding : bindings) {
            String parentUserId = binding.getParentUserId();
            String studentUserId = binding.getStudentUserId();
            Long departmentId = binding.getDepartmentId();

            if (parentUserId == null || studentUserId == null) {
                log.warn("跳过无效的绑定关系: parentUserId={}, studentUserId={}", parentUserId, studentUserId);
                continue;
            }

            // 获取班级名称
            String className = null;
            if (departmentId != null && departmentNameMap.containsKey(departmentId)) {
                String dsedjName = departmentNameMap.get(departmentId);
                log.debug("部门 ID: {}, 部门名称: {}", departmentId, dsedjName);
                if (dsedjName != null && classSectionMap.containsKey(dsedjName)) {
                    ClassSection classSection = classSectionMap.get(dsedjName);
                    className = classSection.getClassSectionSp(); // 使用 SP 班级名
                    log.debug("找到班级: {} -> {}", dsedjName, className);
                } else {
                    log.warn("未找到匹配的班级信息，部门名称: {}", dsedjName);
                }
            } else {
                log.warn("部门 ID 为空或未找到部门名称，departmentId: {}", departmentId);
            }

            // 获取学生姓名
            String studentName = studentNameMap.get(studentUserId);
            if (studentName == null) {
                log.warn("未找到学生姓名，studentUserId: {}", studentUserId);
            }

            log.info("构建消息 - 家长: {}, 学生: {}, 班级: {}, 学生名: {}", 
                    parentUserId, studentUserId, className, studentName);
            messageInfos.add(new ParentStudentMessageInfo(parentUserId, studentUserId, className, studentName));
        }

        log.info("消息信息构建完成，总数: {}", messageInfos.size());
        return messageInfos;
    }

    /**
     * 批量预加载部门名称
     *
     * @param bindings 家长-学生绑定关系列表
     * @return 部门 ID -> 部门名称 的映射
     */
    private Map<Long, String> preloadDepartmentNames(List<SysDepartmentParentBinding> bindings) {
        // 提取所有唯一的部门 ID（过滤掉 null）
        Set<Long> departmentIds = bindings.stream()
                .map(SysDepartmentParentBinding::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (departmentIds.isEmpty()) {
            log.warn("所有绑定的 departmentId 都为 null，无法预加载部门名称");
            return Collections.emptyMap();
        }

        // 查询所有部门信息
        List<SysDepartment> allDepartments = sysDepartmentMapper.selectAll();
        if (allDepartments == null || allDepartments.isEmpty()) {
            return Collections.emptyMap();
        }

        // 构建部门 ID -> 部门名称的映射
        Map<Long, String> departmentNameMap = new HashMap<>();
        for (SysDepartment dept : allDepartments) {
            if (dept != null && dept.getId() != null && dept.getName() != null) {
                departmentNameMap.put(dept.getId(), dept.getName());
            }
        }

        return departmentNameMap;
    }

    /**
     * 批量预加载班级信息
     *
     * @param departmentNameMap 部门名称映射
     * @return DSEDJ 名称 -> ClassSection 的映射
     */
    private Map<String, ClassSection> preloadClassSections(Map<Long, String> departmentNameMap) {
        if (departmentNameMap == null || departmentNameMap.isEmpty()) {
            return Collections.emptyMap();
        }

        // 提取所有唯一的部门名称（DSEDJ）
        Set<String> dsedjNames = departmentNameMap.values().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (dsedjNames.isEmpty()) {
            return Collections.emptyMap();
        }

        // 批量查询班级信息
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
     * 批量预加载学生姓名
     *
     * @param bindings 家长-学生绑定关系列表
     * @return 学生用户 ID -> 学生姓名 的映射
     */
    private Map<String, String> preloadStudentNames(List<SysDepartmentParentBinding> bindings) {

        // 提取所有唯一的学生用户 ID
        Set<String> studentUserIds = bindings.stream()
                .map(SysDepartmentParentBinding::getStudentUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (studentUserIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 通过 sys_parent_student_relation 表查询学生姓名
        // 注意：当前 Mapper 没有直接通过 student_user_id 查询的方法
        // 所以我们先查询所有相关的家长-学生关系，然后过滤
        Set<String> parentUserIds = bindings.stream()
                .map(SysDepartmentParentBinding::getParentUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (parentUserIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 批量查询家长-学生关系
        List<SysParentStudentRelation> relations = parentStudentRelationMapper.selectByParentUserIds(
                new ArrayList<>(parentUserIds));

        if (relations == null || relations.isEmpty()) {
            return Collections.emptyMap();
        }

        // 构建学生用户 ID -> 学生姓名的映射
        Map<String, String> studentNameMap = new HashMap<>();
        for (SysParentStudentRelation relation : relations) {
            if (relation != null && relation.getStudentUserId() != null && relation.getStudentName() != null) {
                studentNameMap.put(relation.getStudentUserId(), relation.getStudentName());
            }
        }

        return studentNameMap;
    }
}
