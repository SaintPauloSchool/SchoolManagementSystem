package com.sp.system.service.impl;

import com.sp.system.entity.SysSchoolDepartment;
import com.sp.system.entity.SysSchoolDepartmentMember;
import com.sp.system.mapper.SysSchoolDepartmentMapper;
import com.sp.system.mapper.SysSchoolDepartmentMemberMapper;
import com.sp.system.service.ISysSchoolDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 学校部门 Service 实现类
 *
 */
@Service
public class SysSchoolDepartmentServiceImpl implements ISysSchoolDepartmentService {

    @Autowired
    private SysSchoolDepartmentMapper schoolDepartmentMapper;

    @Autowired
    private SysSchoolDepartmentMemberMapper schoolDepartmentMemberMapper;

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
                        dept -> Optional.ofNullable(dept.getParentId()).orElse((Integer) 0).longValue()
                ));
    }

    /**
     * 获取根节点列表
     */
    private List<SysSchoolDepartment> getRootNodes(List<SysSchoolDepartment> allDepartments) {
        return allDepartments.stream()
                .filter(dept -> Optional.ofNullable(dept.getParentId()).orElse((Integer) 0) == 0)
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

    /**
     * 为部门加载成员数据
     */
    private void loadMembersForDepartments(List<SysSchoolDepartment> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        // 收集所有需要查询成员的部门 ID
        List<Long> departmentIds = nodes.stream()
                .filter(this::isValidNonLeafDepartment)
                .map(SysSchoolDepartment::getId)
                .distinct()
                .collect(Collectors.toList());

        if (departmentIds.isEmpty()) {
            return;
        }

        // 批量查询所有部门的成员并按部门 ID 分组
        Map<Long, List<SysSchoolDepartmentMember>> membersMap = queryMembersByDepartmentIds(departmentIds);

        // 为每个部门分配成员数据并递归处理子部门
        nodes.stream()
                .filter(Objects::nonNull)
                .filter(dept -> dept.getId() != null)
                .filter(dept -> !Boolean.TRUE.equals(dept.getIsLeaf()))
                .forEach(dept -> processDepartmentMembers(dept, membersMap));
    }

    /**
     * 处理部门成员数据
     */
    private void processDepartmentMembers(SysSchoolDepartment dept, Map<Long, List<SysSchoolDepartmentMember>> membersMap) {
        // 获取部门成员
        List<SysSchoolDepartmentMember> members = membersMap.get(dept.getId());

        if (members == null || members.isEmpty()) {
            return;
        }

        // 转换成员为节点并添加到 children
        List<SysSchoolDepartment> memberNodes = members.stream()
                .filter(Objects::nonNull)
                .map(member -> convertToMemberNode(member, dept.getId()))
                .collect(Collectors.toList());

        if (!memberNodes.isEmpty()) {
            if (dept.getChildren() == null) {
                dept.setChildren(new ArrayList<>(memberNodes));
            } else {
                dept.getChildren().addAll(memberNodes);
            }
        }

        // 递归处理子部门
        if (dept.getChildren() != null) {
            List<SysSchoolDepartment> childDepartments = dept.getChildren().stream()
                    .filter(child -> !Boolean.TRUE.equals(child.getIsLeaf()))
                    .collect(Collectors.toList());

            if (!childDepartments.isEmpty()) {
                loadMembersForDepartments(childDepartments);
            }
        }
    }

    /**
     * 判断是否为有效的非叶子部门节点
     */
    private boolean isValidNonLeafDepartment(SysSchoolDepartment dept) {
        return dept != null
                && dept.getId() != null
                && !Boolean.TRUE.equals(dept.getIsLeaf());
    }

    /**
     * 批量查询部门成员
     */
    private Map<Long, List<SysSchoolDepartmentMember>> queryMembersByDepartmentIds(List<Long> departmentIds) {
        // 查询所有部门成员
        List<SysSchoolDepartmentMember> allMembers = schoolDepartmentMemberMapper.selectMembersByDepartmentIds(departmentIds);
        // 按部门 ID 分组
        return allMembers.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(SysSchoolDepartmentMember::getDepartmentId));
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
