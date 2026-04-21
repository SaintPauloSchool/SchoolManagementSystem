package com.sms.system.service.impl;

import com.sms.system.entity.WecomSchoolDepartment;
import com.sms.system.entity.WecomSchoolDepartmentMember;
import com.sms.system.mapper.WecomSchoolDepartmentMapper;
import com.sms.system.mapper.WecomSchoolDepartmentMemberMapper;
import com.sms.system.service.IWecomSchoolDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * wecom学校部门 Service 实现类
 *
 */
@Service
public class WecomSchoolDepartmentServiceImpl implements IWecomSchoolDepartmentService {

    @Autowired
    private WecomSchoolDepartmentMapper schoolDepartmentMapper;

    @Autowired
    private WecomSchoolDepartmentMemberMapper schoolDepartmentMemberMapper;

    /**
     * 获取学校部门树形结构（带成员）
     */
    @Override
    public List<WecomSchoolDepartment> getWecomSchoolDepartmentTreeWithMembers() {
        List<WecomSchoolDepartment> rootNodes = buildDepartmentTree();
        loadMembersForDepartments(rootNodes);
        return rootNodes;
    }

    /**
     * 获取学校部门树形结构（仅部门，不含人员）
     */
    @Override
    public List<WecomSchoolDepartment> getWecomSchoolDepartmentTree() {
        return buildDepartmentTree();
    }

    /**
     * 构建部门树形结构
     */
    private List<WecomSchoolDepartment> buildDepartmentTree() {
        // 1. 查询所有部门数据
        List<WecomSchoolDepartment> allDepartments = schoolDepartmentMapper.selectAll();

        if (allDepartments == null || allDepartments.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 构建父子关系映射
        Map<Long, List<WecomSchoolDepartment>> childrenMap = buildChildrenMap(allDepartments);

        // 3. 找到根节点（parentId 为 null 或 0）
        List<WecomSchoolDepartment> rootNodes = getRootNodes(allDepartments);

        // 4. 递归构建树形结构
        buildTree(rootNodes, childrenMap);

        return rootNodes;
    }

    /**
     * 构建父子关系映射
     */
    private Map<Long, List<WecomSchoolDepartment>> buildChildrenMap(List<WecomSchoolDepartment> allDepartments) {
        return allDepartments.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        dept -> Optional.ofNullable(dept.getParentId()).orElse((Integer) 0).longValue()
                ));
    }

    /**
     * 获取根节点列表
     */
    private List<WecomSchoolDepartment> getRootNodes(List<WecomSchoolDepartment> allDepartments) {
        return allDepartments.stream()
                .filter(dept -> Optional.ofNullable(dept.getParentId()).orElse((Integer) 0) == 0)
                .collect(Collectors.toList());
    }

    /**
     * 递归构建树形结构
     */
    private void buildTree(List<WecomSchoolDepartment> nodes, Map<Long, List<WecomSchoolDepartment>> childrenMap) {
        nodes.stream()
                .filter(Objects::nonNull)
                .filter(node -> node.getId() != null)
                .forEach(node -> {
                    List<WecomSchoolDepartment> children = childrenMap.get(node.getId());
                    if (children != null && !children.isEmpty()) {
                        node.setChildren(children);
                        buildTree(children, childrenMap);
                    }
                });
    }

    /**
     * 为部门加载成员数据
     */
    private void loadMembersForDepartments(List<WecomSchoolDepartment> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        // 收集所有需要查询成员的部门 ID
        List<Long> departmentIds = nodes.stream()
                .filter(this::isValidNonLeafDepartment)
                .map(WecomSchoolDepartment::getId)
                .distinct()
                .collect(Collectors.toList());

        if (departmentIds.isEmpty()) {
            return;
        }

        // 批量查询所有部门的成员并按部门 ID 分组
        Map<Long, List<WecomSchoolDepartmentMember>> membersMap = queryMembersByDepartmentIds(departmentIds);

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
    private void processDepartmentMembers(WecomSchoolDepartment dept, Map<Long, List<WecomSchoolDepartmentMember>> membersMap) {
        // 获取部门成员
        List<WecomSchoolDepartmentMember> members = membersMap.get(dept.getId());

        if (members == null || members.isEmpty()) {
            return;
        }

        // 转换成员为节点并添加到 children
        List<WecomSchoolDepartment> memberNodes = members.stream()
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
            List<WecomSchoolDepartment> childDepartments = dept.getChildren().stream()
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
    private boolean isValidNonLeafDepartment(WecomSchoolDepartment dept) {
        return dept != null
                && dept.getId() != null
                && !Boolean.TRUE.equals(dept.getIsLeaf());
    }

    /**
     * 批量查询部门成员
     */
    private Map<Long, List<WecomSchoolDepartmentMember>> queryMembersByDepartmentIds(List<Long> departmentIds) {
        // 查询所有部门成员
        List<WecomSchoolDepartmentMember> allMembers = schoolDepartmentMemberMapper.selectMembersByDepartmentIds(departmentIds);
        // 按部门 ID 分组
        return allMembers.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(WecomSchoolDepartmentMember::getDepartmentId));
    }

    /**
     * 将成员转换为部门节点（用于树形展示）
     */
    private WecomSchoolDepartment convertToMemberNode(WecomSchoolDepartmentMember member, Long currentDepartmentId) {
        WecomSchoolDepartment node = new WecomSchoolDepartment();
        node.setId(member.getId());
        node.setName(member.getName());
        node.setParentId(currentDepartmentId.intValue());
        node.setIsLeaf(true);
        node.setStaffUserId(member.getUserid());
        return node;
    }
}
