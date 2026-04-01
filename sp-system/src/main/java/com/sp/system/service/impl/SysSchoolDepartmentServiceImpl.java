package com.sp.system.service.impl;

import com.sp.system.entity.SysSchoolDepartment;
import com.sp.system.mapper.SysSchoolDepartmentMapper;
import com.sp.system.service.ISysSchoolDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统学校部门 Service 实现类
 *
 */
@Service
public class SysSchoolDepartmentServiceImpl implements ISysSchoolDepartmentService {

    @Autowired
    private SysSchoolDepartmentMapper schoolDepartmentMapper;

    /**
     * 获取学校部门树形结构（仅部门，不含人员）
     */
    @Override
    public List<SysSchoolDepartment> getSysSchoolDepartmentTree() {
        return buildDepartmentTree();
    }

    /**
     * 构建部门树形结构
     */
    private List<SysSchoolDepartment> buildDepartmentTree() {
        // 1. 查询所有部门数据
        List<SysSchoolDepartment> allDepartments = schoolDepartmentMapper.selectAll();
        
        if (allDepartments == null || allDepartments.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 构建父子关系映射
        Map<Long, List<SysSchoolDepartment>> childrenMap = buildChildrenMap(allDepartments);

        // 3. 找到根节点（parentId 为 null 或 0）
        List<SysSchoolDepartment> rootNodes = getRootNodes(allDepartments);

        // 4. 递归构建树形结构
        buildTree(rootNodes, childrenMap);

        return rootNodes;
    }

    /**
     * 构建父子关系映射
     */
    private Map<Long, List<SysSchoolDepartment>> buildChildrenMap(List<SysSchoolDepartment> allDepartments) {
        return allDepartments.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        dept -> Optional.ofNullable(dept.getParentId()).orElse(0).longValue()
                ));
    }

    /**
     * 获取根节点列表
     */
    private List<SysSchoolDepartment> getRootNodes(List<SysSchoolDepartment> allDepartments) {
        return allDepartments.stream()
                .filter(dept -> Optional.ofNullable(dept.getParentId()).orElse(0) == 0)
                .collect(Collectors.toList());
    }

    /**
     * 递归构建树形结构
     */
    private void buildTree(List<SysSchoolDepartment> nodes, Map<Long, List<SysSchoolDepartment>> childrenMap) {
        nodes.stream()
                .filter(Objects::nonNull)
                .filter(node -> node.getId() != null)
                .forEach(node -> {
                    List<SysSchoolDepartment> children = childrenMap.get(node.getId());
                    if (children != null && !children.isEmpty()) {
                        node.setChildren(children);
                        buildTree(children, childrenMap);
                    }
                });
    }
}
