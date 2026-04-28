package com.sms.system.service.impl;

import com.sms.system.entity.SysSchoolDepartment;
import com.sms.system.entity.SysSchoolDepartmentMember;
import com.sms.system.mapper.SysSchoolDepartmentMapper;
import com.sms.system.mapper.SysSchoolDepartmentMemberMapper;
import com.sms.system.service.ISysSchoolDepartmentService;
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

    @Autowired
    private SysSchoolDepartmentMemberMapper schoolDepartmentMemberMapper;

    /**
     * 获取学校部门树形结构（仅部门，不含人员）
     */
    @Override
    public List<SysSchoolDepartment> getSysSchoolDepartmentTree(Integer type) {
        return buildDepartmentTree(type);
    }

    /**
     * 获取学校部门树形结构（包含人员作为叶子节点）
     */
    @Override
    public List<SysSchoolDepartment> getSysSchoolDepartmentTreeWithMembers(Integer type) {
        // 1. 获取基础部门树
        List<SysSchoolDepartment> rootNodes = buildDepartmentTree(type);
        if (rootNodes == null || rootNodes.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 收集所有部门节点
        List<SysSchoolDepartment> allDepartments = new ArrayList<>();
        collectAllDepartments(rootNodes, allDepartments);

        List<Long> departmentIds = allDepartments.stream()
                .map(SysSchoolDepartment::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (departmentIds.isEmpty()) {
            return rootNodes;
        }

        // 3. 批量查询所有部门成员
        List<SysSchoolDepartmentMember> members = schoolDepartmentMemberMapper.selectMembersByDepartmentIds(departmentIds);
        if (members == null || members.isEmpty()) {
            return rootNodes;
        }

        // 4. 按部门分组
        Map<Long, List<SysSchoolDepartmentMember>> membersByDeptMap = members.stream()
                .filter(Objects::nonNull)
                .filter(m -> m.getDepartmentId() != null)
                .collect(Collectors.groupingBy(SysSchoolDepartmentMember::getDepartmentId));

        // 5. 将成员附加到对应部门的 children 目录下
        for (SysSchoolDepartment dept : allDepartments) {
            List<SysSchoolDepartmentMember> deptMembers = membersByDeptMap.get(dept.getId());
            if (deptMembers != null && !deptMembers.isEmpty()) {
                if (dept.getChildren() == null) {
                    dept.setChildren(new ArrayList<>());
                }
                for (SysSchoolDepartmentMember member : deptMembers) {
                    dept.getChildren().add(convertMemberToNode(member));
                }
            }
        }

        return rootNodes;
    }

    private void collectAllDepartments(List<SysSchoolDepartment> nodes, List<SysSchoolDepartment> allDepartments) {
        if (nodes == null) return;
        for (SysSchoolDepartment node : nodes) {
            if (node != null) {
                allDepartments.add(node);
                collectAllDepartments(node.getChildren(), allDepartments);
            }
        }
    }

    private SysSchoolDepartment convertMemberToNode(SysSchoolDepartmentMember member) {
        SysSchoolDepartment node = new SysSchoolDepartment();
        // 使用负数 ID 防止跟部门 ID 冲突
        node.setId(-member.getId());
        node.setName(member.getName());
        node.setIsLeaf(true);
        // 可选保留其他必要字段...
        return node;
    }

    /**
     * 构建部门树形结构
     */
    private List<SysSchoolDepartment> buildDepartmentTree(Integer type) {
        // 1. 查询所有部门数据
        List<SysSchoolDepartment> allDepartments = schoolDepartmentMapper.selectAll(type);
        
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

    /**
     * 根据 ID 删除学校部门
     * 同时删除该部门下的所有子部门和成员
     */
    @Override
    public int deleteSysSchoolDepartmentById(Long id) {
        // 1. 先查詢目標部門，取得它的 type
        SysSchoolDepartment targetDept = schoolDepartmentMapper.selectById(id);
        if (targetDept == null) {
            return 0;
        }
        
        // 2. 只查詢相同 type 的部門，避免將所有不同類型的部門資料也一起拉進記憶體
        List<SysSchoolDepartment> allDepartments = schoolDepartmentMapper.selectAll(targetDept.getType());
        if (allDepartments == null || allDepartments.isEmpty()) {
            return 0;
        }

        // 3. 收集需要删除的部门 ID（包括自身和所有子部门）
        List<Long> departmentIdsToDelete = new ArrayList<>();
        collectDepartmentIdsToDelete(id, allDepartments, departmentIdsToDelete);

        if (departmentIdsToDelete.isEmpty()) {
            return 0;
        }

        // 3. 批量删除部门
        int result = schoolDepartmentMapper.deleteByIds(departmentIdsToDelete.toArray(new Long[0]));

        // 4. 删除相关部门成员（通过部门 ID 关联的成员）
        for (Long deptId : departmentIdsToDelete) {
            schoolDepartmentMemberMapper.deleteByDepartmentId(deptId);
        }

        return result;
    }

    /**
     * 新增部门
     */
    @Override
    public int insertSysSchoolDepartment(SysSchoolDepartment department) {
        return schoolDepartmentMapper.insertDepartment(department);
    }

    /**
     * 修改部门
     */
    @Override
    public int updateSysSchoolDepartment(SysSchoolDepartment department) {
        return schoolDepartmentMapper.updateDepartment(department);
    }

    /**
     * 根据 ID 查询部门
     */
    @Override
    public SysSchoolDepartment selectSysSchoolDepartmentById(Long id) {
        return schoolDepartmentMapper.selectById(id);
    }

    /**
     * 递归收集需要删除的部门 ID
     */
    private  void collectDepartmentIdsToDelete(Long parentId, List<SysSchoolDepartment> allDepartments,
                                               List<Long> idsToCollect) {
        idsToCollect.add(parentId);
        
        // 找到所有以当前部门为父部门的子部门
        List<SysSchoolDepartment> children = allDepartments.stream()
                .filter(dept -> dept != null && dept.getParentId() != null)
                .filter(dept -> dept.getParentId().longValue() == parentId)
                .collect(Collectors.toList());
        
        // 递归处理子部门
        for (SysSchoolDepartment child : children) {
            if (child.getId() != null) {
                collectDepartmentIdsToDelete(child.getId(), allDepartments, idsToCollect);
            }
        }
    }

    /**
     * 遞歸獲取 Sys 部門及其所有子孫部門的 ID（自動查詢部門數據）
     *
     * @param departmentIds 部門 ID 列表
     * @param type 部門類型（1 学校部门通讯录, 2 家校通訊錄）
     * @return 所有部門 ID 列表（包括傳入的部門及其所有子孫部門）
     */
    public List<Long> resolveAllDescendantDepartmentIdsByType(List<Long> departmentIds, Integer type) {
        // 如果傳入的部門 ID 列表為空，則返回空列表
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 查詢所有 Sys 部門信息
        List<SysSchoolDepartment> allDepartments = schoolDepartmentMapper.selectAll(type);

        // 對每個傳入的部門 ID，遞歸查找其所有子孫部門
        Set<Long> allDepartmentIds = new HashSet<>(departmentIds);
        for (Long deptId : departmentIds) {
            collectAllDescendantDepartmentIds(deptId, allDepartments, allDepartmentIds);
        }

        return new ArrayList<>(allDepartmentIds);
    }

    /**
     * 遞歸收集某個部門的所有子孫部門 ID
     *
     * @param parentId 父部門 ID
     * @param allDepartments 所有部門列表
     * @param allDepartmentIds 收集結果的集合
     */
    private void collectAllDescendantDepartmentIds(Long parentId, List<SysSchoolDepartment> allDepartments, Set<Long> allDepartmentIds) {
        if (parentId == null || allDepartments == null) {
            return;
        }

        // 找到所有直接子部門
        List<SysSchoolDepartment> children = allDepartments.stream()
                .filter(Objects::nonNull)
                .filter(dept -> dept.getParentId() != null)
                .filter(dept -> dept.getParentId().longValue() == parentId)
                .collect(Collectors.toList());

        for (SysSchoolDepartment child : children) {
            if (child.getId() == null) {
                continue;
            }

            // 添加子部門 ID
            allDepartmentIds.add(child.getId());
            
            // 繼續遞歸查找孫部門
            collectAllDescendantDepartmentIds(child.getId(), allDepartments, allDepartmentIds);
        }
    }
}
