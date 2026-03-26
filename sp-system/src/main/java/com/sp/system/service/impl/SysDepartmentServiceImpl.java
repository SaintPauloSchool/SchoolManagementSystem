package com.sp.system.service.impl;

import com.sp.system.entity.SysDepartment;
import com.sp.system.entity.SysDepartmentParentBinding;
import com.sp.system.entity.SysParentStudentRelation;
import com.sp.system.mapper.SysDepartmentMapper;
import com.sp.system.mapper.SysDepartmentParentBindingMapper;
import com.sp.system.mapper.SysParentStudentRelationMapper;
import com.sp.system.service.ISysDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门 Service 业务层处理
 *
 */
@Service
public class SysDepartmentServiceImpl implements ISysDepartmentService {

    /** 部门类型常量 */
    private static final int TYPE_CLASS = 1;      // 班级
    private static final int TYPE_GRADE = 2;      // 年级
    private static final int TYPE_SCHOOL_SEGMENT = 3;  // 学段
    private static final int TYPE_CAMPUS = 4;     // 校区
    private static final int TYPE_SCHOOL = 5;     // 学校

    @Autowired
    private SysDepartmentMapper departmentMapper;

    @Autowired
    private SysDepartmentParentBindingMapper parentBindingMapper;

    @Autowired
    private SysParentStudentRelationMapper parentStudentRelationMapper;

    /**
     * 获取班级树形结构（使用 Stream 流构建）
     * 层级顺序：type 5(学校) → type 4(校区) → type 3(学段) → type 2(年级) → type 1(班级)
     *
     * @return 学校层级的树形结构
     */
    @Override
    public List<SysDepartment> getClassTree() {
        // 1. 查询所有部门数据
        List<SysDepartment> allDepartments = departmentMapper.selectAll();

        if (allDepartments == null || allDepartments.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 按 type 分组，便于快速查找
        Map<Integer, List<SysDepartment>> departmentsByType = allDepartments.stream()
                .filter(Objects::nonNull)
                .filter(dept -> dept.getType() != null)
                .collect(Collectors.groupingBy(SysDepartment::getType));

        // 3. 从最高层级开始构建（优先学校，其次校区）
        List<SysDepartment> rootNodes = departmentsByType.getOrDefault(TYPE_SCHOOL, Collections.emptyList());
        
        if (rootNodes.isEmpty()) {
            // 如果没有学校，尝试从校区开始
            rootNodes = departmentsByType.getOrDefault(TYPE_CAMPUS, Collections.emptyList());
            if (!rootNodes.isEmpty()) {
                buildAndSortTree(rootNodes, departmentsByType, TYPE_CAMPUS);
                return rootNodes;
            }
            return Collections.emptyList();
        }

        // 4. 构建树形结构并排序
        buildAndSortTree(rootNodes, departmentsByType, TYPE_SCHOOL);

        return rootNodes;
    }

    /**
     * 获取班级树形结构（带家长学生关系，用于学生/家长选择器）
     * 在 getClassTree 的基础上为 type=1 的班级加载家长学生关系数据
     *
     * @return 带家长学生关系的树形结构
     */
    @Override
    public List<SysDepartment> getClassTreeWithParents() {
        // 1. 获取基础树形结构
        List<SysDepartment> tree = getClassTree();
        
        // 2. 为 type=1 的班级添加家长学生关系数据
        if (tree != null && !tree.isEmpty()) {
            loadParentStudentRelations(tree);
        }
        
        return tree;
    }

    /**
     * 构建树形结构并排序（合并了构建和排序逻辑）
     * 从指定类型开始，逐级向下构建，并对每层进行排序
     *
     * @param currentLevel 当前层级的部门列表
     * @param departmentsByType 按类型分组的部门 Map
     * @param currentType 当前层级类型
     */
    private void buildAndSortTree(List<SysDepartment> currentLevel, 
                                   Map<Integer, List<SysDepartment>> departmentsByType,
                                   Integer currentType) {
        if (currentLevel == null || currentLevel.isEmpty()) {
            return;
        }

        // 对当前层级排序（按名称字母顺序）
        currentLevel.sort(Comparator.comparing(
                dept -> dept.getName() != null ? dept.getName() : "",
                String.CASE_INSENSITIVE_ORDER
        ));

        // 下一个层级（type 递减：5→4→3→2→1）
        Integer nextType = currentType - 1;
        
        // 如果没有下一个层级或者下一个层级没有数据，返回
        if (nextType < TYPE_CLASS || !departmentsByType.containsKey(nextType)) {
            return;
        }

        List<SysDepartment> nextLevelDepartments = departmentsByType.get(nextType);
        if (nextLevelDepartments == null) {
            return;
        }
        
        // 为当前层级的每个部门设置子部门
        for (SysDepartment currentDept : currentLevel) {
            if (currentDept == null || currentDept.getId() == null) {
                continue;
            }
            
            long currentId = currentDept.getId();
            List<SysDepartment> children = nextLevelDepartments.stream()
                    .filter(Objects::nonNull)
                    .filter(dept -> dept.getParentId() != null) 
                    .filter(dept -> dept.getParentId().longValue() == currentId)
                    .collect(Collectors.toList());

            if (children != null && !children.isEmpty()) {
                currentDept.setChildren(children);
                
                // 递归处理下一层级
                if (nextType > TYPE_CLASS) {
                    buildAndSortTree(children, departmentsByType, nextType);
                }
            }
        }
    }

    /**
     * 为班级节点加载家长学生关系数据
     * 为每个家长创建独立的节点
     *
     * @param nodes 部门节点列表
     */
    private void loadParentStudentRelations(List<SysDepartment> nodes) {
        // 遍历部门节点
        for (SysDepartment dept : nodes) {
            if (dept == null) {
                continue;
            }
            
            // 如果是班级节点（type=1），加载家长学生关系
            if (dept.getType() != null && dept.getType() == TYPE_CLASS) {
                loadParentsForClass(dept);
            }
            
            // 递归处理子节点
            if (dept.getChildren() != null && !dept.getChildren().isEmpty()) {
                loadParentStudentRelations(dept.getChildren());
            }
        }
    }

    /**
     * 为单个班级加载家长节点
     *
     * @param classDept 班级部门
     */
    private void loadParentsForClass(SysDepartment classDept) {
        if (classDept.getId() == null) {
            return;
        }
        
        // 通过部门 ID 查询家长绑定关系
        List<SysDepartmentParentBinding> bindings = parentBindingMapper.selectByDepartmentId(classDept.getId());
        
        if (bindings == null || bindings.isEmpty()) {
            return;
        }
        
        // 获取所有不重复的家长用户 ID
        List<String> parentUserIds = bindings.stream()
                .filter(Objects::nonNull)
                .map(SysDepartmentParentBinding::getParentUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        if (parentUserIds.isEmpty()) {
            return;
        }
        
        // 批量查询家长学生关系
        List<SysParentStudentRelation> relations = parentStudentRelationMapper.selectByParentUserIds(parentUserIds);
        
        if (relations == null || relations.isEmpty()) {
            return;
        }
        
        // 初始化 children 列表
        if (classDept.getChildren() == null) {
            classDept.setChildren(new ArrayList<>());
        }
        
        // 为每个家长关系创建独立节点
        for (SysParentStudentRelation relation : relations) {
            if (relation != null) {
                SysDepartment node = convertToDepartmentNode(relation);
                classDept.getChildren().add(node);
            }
        }
    }

    /**
     * 将家长学生关系转换为部门节点
     * 节点名称格式：学生姓名 - 关系描述（如：张三 - 父亲）
     *
     * @param relation 家长学生关系
     * @return 部门节点
     */
    private SysDepartment convertToDepartmentNode(SysParentStudentRelation relation) {
        // 创建部门节点
        SysDepartment node = new SysDepartment();
        node.setId(relation.getId());
        node.setStudentUserId(relation.getStudentUserId());
        node.setParentUserId(relation.getParentUserId());
        
        // 构建显示名称：学生姓名 - 关系描述
        String studentName = relation.getStudentName() != null ? relation.getStudentName() : "未知";
        String relationDesc = relation.getRelationDesc() != null ? relation.getRelationDesc() : "";
        node.setName(studentName + (relationDesc.isEmpty() ? "" : "-" + relationDesc));
        
        node.setRelationDesc(relationDesc);
        node.setMobile(relation.getMobile());
        node.setIsLeaf(true);
        return node;
    }
}
