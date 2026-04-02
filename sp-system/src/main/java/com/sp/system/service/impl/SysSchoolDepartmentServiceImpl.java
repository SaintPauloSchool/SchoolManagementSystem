package com.sp.system.service.impl;

import com.sp.system.entity.SysSchoolDepartment;
import com.sp.system.mapper.SysSchoolDepartmentMapper;
import com.sp.system.mapper.SysSchoolDepartmentMemberMapper;
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
}
