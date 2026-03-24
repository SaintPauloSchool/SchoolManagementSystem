package com.sp.system.service.impl;

import com.sp.common.core.domain.entity.SysDepartment;
import com.sp.system.mapper.SysDepartmentMapper;
import com.sp.system.service.ISysDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门 Service 业务层处理
 *
 */
@Service
public class SysDepartmentServiceImpl implements ISysDepartmentService {

    @Autowired
    private SysDepartmentMapper departmentMapper;

    /**
     * 根据类型查询部门列表
     *
     * @param type 部门类型
     * @return 部门集合
     */
    @Override
    public List<SysDepartment> selectByType(Integer type) {
        return departmentMapper.selectByType(type);
    }

    /**
     * 根据父级 ID 查询子部门
     *
     * @param parentId 父级 ID
     * @return 部门集合
     */
    @Override
    public List<SysDepartment> selectByParentId(Integer parentId) {
        return departmentMapper.selectByParentId(parentId);
    }

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

        if (allDepartments.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 按 type 分组，便于快速查找
        Map<Integer, List<SysDepartment>> departmentsByType = allDepartments.stream()
                .collect(Collectors.groupingBy(SysDepartment::getType));

        // 3. 获取 type=5（学校）的部门作为根节点
        List<SysDepartment> schools = departmentsByType.getOrDefault(5, Collections.emptyList());
        if (schools.isEmpty()) {
            // 如果没有学校，尝试从 type=4 开始
            List<SysDepartment> campuses = departmentsByType.getOrDefault(4, Collections.emptyList());
            if (!campuses.isEmpty()) {
                buildTreeRecursive(campuses, departmentsByType, 4);
                return campuses;
            }
            return Collections.emptyList();
        }

        // 4. 递归构建树形结构
        buildTreeRecursive(schools, departmentsByType, 5);

        // 5. 对所有层级进行排序
        sortTreeRecursive(schools);

        return schools;
    }

    /**
     * 递归排序树形结构
     * type=2（年级）按 id 排序
     * 其他类型按 name 排序
     *
     * @param nodes 部门节点列表
     */
    private void sortTreeRecursive(List<SysDepartment> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        // 对当前层级排序
        nodes.sort((dept1, dept2) -> {
            // 其他类型按 name 排序
            String name1 = dept1.getName() != null ? dept1.getName() : "";
            String name2 = dept2.getName() != null ? dept2.getName() : "";
            return name1.compareTo(name2);
        });

        // 递归排序子节点
        for (SysDepartment dept : nodes) {
            if (dept.getChildren() != null && !dept.getChildren().isEmpty()) {
                sortTreeRecursive(dept.getChildren());
            }
        }
    }

    /**
     * 递归构建树形结构
     *
     * @param currentLevel 当前层级的部门列表
     * @param departmentsByType 按类型分组的部门 Map
     * @param currentType 当前层级类型
     */
    private void buildTreeRecursive(List<SysDepartment> currentLevel, 
                                    Map<Integer, List<SysDepartment>> departmentsByType,
                                    Integer currentType) {
        // 下一个层级（type 递减：5→4→3→2→1）
        Integer nextType = currentType - 1;
        
        // 如果没有下一个层级或者下一个层级没有数据，返回
        if (nextType < 1 || !departmentsByType.containsKey(nextType)) {
            return;
        }

        List<SysDepartment> nextLevelDepartments = departmentsByType.get(nextType);
        
        // 为当前层级的每个部门设置子部门
        for (SysDepartment currentDept : currentLevel) {
            // 使用 Stream 流筛选出当前部门的子部门
            // 注意：将 Long 类型的 id 转换为 long 值进行比较，避免 Integer 和 Long 直接 equals
            long currentId = currentDept.getId() != null ? currentDept.getId() : 0L;
            List<SysDepartment> children = nextLevelDepartments.stream()
                    .filter(dept -> dept.getParentId() != null && 
                                   dept.getParentId().longValue() == currentId)
                    .collect(Collectors.toList());

            if (!children.isEmpty()) {
                currentDept.setChildren(children);
                
                // 递归处理下一层级
                if (nextType > 1) {
                    buildTreeRecursive(children, departmentsByType, nextType);
                }
            }
        }
    }

    /**
     * 查询所有班级（type=1）
     *
     * @return 班级集合
     */
    @Override
    public List<SysDepartment> selectAllClasses() {
        return selectByType(1);
    }
}
