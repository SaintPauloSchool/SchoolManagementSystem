package com.sp.system.service.impl;

import com.sp.system.entity.SysSchoolDepartment;
import com.sp.system.entity.SysSchoolDepartmentMember;
import com.sp.system.mapper.SysSchoolDepartmentMapper;
import com.sp.system.service.ISysSchoolDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 学校部门 Service 实现类
 *
 */
@Service
public class SysSchoolDepartmentServiceImpl implements ISysSchoolDepartmentService {

    @Autowired
    private SysSchoolDepartmentMapper schoolDepartmentMapper;

    /**
     * 获取学校部门树形结构（带成员）
     */
    @Override
    public List<SysSchoolDepartment> getSchoolDepartmentTreeWithMembers() {
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

        // 5. 为每个部门加载成员数据
        loadMembersForDepartments(rootNodes);

        return rootNodes;
    }

    /**
     * 构建父子关系映射
     */
    private Map<Long, List<SysSchoolDepartment>> buildChildrenMap(List<SysSchoolDepartment> allDepartments) {
        return allDepartments.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        dept -> dept.getParentId() != null ? dept.getParentId().longValue() : 0L
                ));
    }

    /**
     * 获取根节点列表
     */
    private List<SysSchoolDepartment> getRootNodes(List<SysSchoolDepartment> allDepartments) {
        return allDepartments.stream()
                .filter(dept -> dept.getParentId() == null || dept.getParentId() == 0)
                .sorted(Comparator.comparingInt((SysSchoolDepartment dept) -> dept.getOrderNum() != null ? dept.getOrderNum() : 0).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 递归构建树形结构
     */
    private void buildTree(List<SysSchoolDepartment> nodes, Map<Long, List<SysSchoolDepartment>> childrenMap) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        for (SysSchoolDepartment node : nodes) {
            if (node == null || node.getId() == null) {
                continue;
            }

            // 按 orderNum 倒序排序子节点
            List<SysSchoolDepartment> children = childrenMap.getOrDefault(node.getId(), Collections.emptyList());
            if (!children.isEmpty()) {
                children.sort(Comparator.comparingInt((SysSchoolDepartment dept) -> dept.getOrderNum() != null ? dept.getOrderNum() : 0).reversed());
                node.setChildren(children);
                buildTree(children, childrenMap);
            }
        }
    }

    /**
     * 为部门加载成员数据
     */
    private void loadMembersForDepartments(List<SysSchoolDepartment> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        for (SysSchoolDepartment dept : nodes) {
            if (dept == null || dept.getId() == null || Boolean.TRUE.equals(dept.getIsLeaf())) {
                continue;
            }

            // 查询部门成员
            List<SysSchoolDepartmentMember> members = schoolDepartmentMapper.selectMembersByDepartmentId(dept.getId());
            if (members == null || members.isEmpty()) {
                continue;
            }

            // 初始化 children 列表
            if (dept.getChildren() == null) {
                dept.setChildren(new ArrayList<>());
            }

            // 为每个成员创建叶子节点
            for (SysSchoolDepartmentMember member : members) {
                if (member != null) {
                    dept.getChildren().add(convertToMemberNode(member, dept.getId()));
                }
            }

            // 递归处理子部门节点（只处理非叶子节点）
            List<SysSchoolDepartment> childDepartments = dept.getChildren().stream()
                    .filter(child -> !Boolean.TRUE.equals(child.getIsLeaf()))
                    .collect(Collectors.toList());
            
            if (!childDepartments.isEmpty()) {
                loadMembersForDepartments(childDepartments);
            }
        }
    }

    /**
     * 将成员转换为部门节点（用于树形展示）
     */
    private SysSchoolDepartment convertToMemberNode(SysSchoolDepartmentMember member, Long currentDepartmentId) {
        SysSchoolDepartment node = new SysSchoolDepartment();
        node.setId(member.getId());
        node.setName(member.getName());
        node.setParentId(currentDepartmentId.intValue());
        node.setIsLeaf(true);
        node.setStaffUserId(member.getUserid());
        return node;
    }
}
