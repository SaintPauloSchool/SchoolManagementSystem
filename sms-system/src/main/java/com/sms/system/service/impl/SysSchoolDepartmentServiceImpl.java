package com.sms.system.service.impl;

import com.sms.common.utils.bean.BeanCopyUtils;
import com.sms.system.entity.SysSchoolDepartment;
import com.sms.system.entity.SysSchoolDepartmentMember;
import com.sms.system.entity.dto.SysSchoolDepartmentSaveDTO;
import com.sms.system.entity.vo.SysSchoolDepartmentVO;
import com.sms.system.mapper.SysSchoolDepartmentMapper;
import com.sms.system.mapper.SysSchoolDepartmentMemberMapper;
import com.sms.system.service.ISysSchoolDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系統學校部門 Service 實現類
 *
 */
@Service
public class SysSchoolDepartmentServiceImpl implements ISysSchoolDepartmentService {

    @Autowired
    private SysSchoolDepartmentMapper schoolDepartmentMapper;

    @Autowired
    private SysSchoolDepartmentMemberMapper schoolDepartmentMemberMapper;

    /**
     * 獲取學校部門樹形結構（僅部門，不含人員）
     */
    @Override
    public List<SysSchoolDepartmentVO> getSysSchoolDepartmentTree(Integer type) {
        return BeanCopyUtils.copyTree(buildDepartmentTree(type), SysSchoolDepartmentVO.class,
                SysSchoolDepartment::getChildren, SysSchoolDepartmentVO::setChildren);
    }

    /**
     * 獲取學校部門樹形結構（包含人員作爲葉子節點）
     */
    @Override
    public List<SysSchoolDepartmentVO> getSysSchoolDepartmentTreeWithMembers(Integer type) {
        // 1. 獲取基礎部門樹
        List<SysSchoolDepartment> rootNodes = buildDepartmentTree(type);
        if (rootNodes == null || rootNodes.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 收集所有部門節點
        List<SysSchoolDepartment> allDepartments = new ArrayList<>();
        collectAllDepartments(rootNodes, allDepartments);

        List<Long> departmentIds = allDepartments.stream()
                .map(SysSchoolDepartment::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (departmentIds.isEmpty()) {
            return BeanCopyUtils.copyTree(rootNodes, SysSchoolDepartmentVO.class,
                    SysSchoolDepartment::getChildren, SysSchoolDepartmentVO::setChildren);
        }

        // 3. 批量查詢所有部門成員
        List<SysSchoolDepartmentMember> members = schoolDepartmentMemberMapper.selectMembersByDepartmentIds(departmentIds);
        if (members == null || members.isEmpty()) {
            return BeanCopyUtils.copyTree(rootNodes, SysSchoolDepartmentVO.class,
                    SysSchoolDepartment::getChildren, SysSchoolDepartmentVO::setChildren);
        }

        // 4. 按部門分組
        Map<Long, List<SysSchoolDepartmentMember>> membersByDeptMap = members.stream()
                .filter(Objects::nonNull)
                .filter(m -> m.getDepartmentId() != null)
                .collect(Collectors.groupingBy(SysSchoolDepartmentMember::getDepartmentId));

        // 5. 將成員附加到對應部門的 children 目錄下
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

        return BeanCopyUtils.copyTree(rootNodes, SysSchoolDepartmentVO.class,
                SysSchoolDepartment::getChildren, SysSchoolDepartmentVO::setChildren);
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
        // 使用負數 ID 防止跟部門 ID 衝突
        node.setId(-member.getId());
        node.setName(member.getName());
        node.setIsLeaf(true);
        node.setClassDepartmentId(member.getDepartmentId());
        return node;
    }

    /**
     * 構建部門樹形結構
     */
    private List<SysSchoolDepartment> buildDepartmentTree(Integer type) {
        // 1. 查詢所有部門數據
        List<SysSchoolDepartment> allDepartments = schoolDepartmentMapper.selectAll(type);
        
        if (allDepartments == null || allDepartments.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 構建父子關係映射
        Map<Long, List<SysSchoolDepartment>> childrenMap = buildChildrenMap(allDepartments);

        // 3. 找到根節點（parentId 爲 null 或 0）
        List<SysSchoolDepartment> rootNodes = getRootNodes(allDepartments);

        // 4. 遞歸構建樹形結構
        buildTree(rootNodes, childrenMap);

        return rootNodes;
    }

    /**
     * 構建父子關係映射
     */
    private Map<Long, List<SysSchoolDepartment>> buildChildrenMap(List<SysSchoolDepartment> allDepartments) {
        return allDepartments.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        dept -> Optional.ofNullable(dept.getParentId()).orElse(0).longValue()
                ));
    }

    /**
     * 獲取根節點列表
     */
    private List<SysSchoolDepartment> getRootNodes(List<SysSchoolDepartment> allDepartments) {
        return allDepartments.stream()
                .filter(dept -> Optional.ofNullable(dept.getParentId()).orElse(0) == 0)
                .collect(Collectors.toList());
    }

    /**
     * 遞歸構建樹形結構
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
     * 根據 ID 刪除學校部門
     * 同時刪除該部門下的所有子部門和成員
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
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

        // 3. 收集需要刪除的部門 ID（包括自身和所有子部門）
        List<Long> departmentIdsToDelete = new ArrayList<>();
        collectDepartmentIdsToDelete(id, allDepartments, departmentIdsToDelete);

        if (departmentIdsToDelete.isEmpty()) {
            return 0;
        }

        // 3. 批量刪除部門
        int result = schoolDepartmentMapper.deleteByIds(departmentIdsToDelete.toArray(new Long[0]));

        // 4. 刪除相關部門成員（通過部門 ID 關聯的成員）
        for (Long deptId : departmentIdsToDelete) {
            schoolDepartmentMemberMapper.deleteByDepartmentId(deptId);
        }

        return result;
    }

    /**
     * 新增部門
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertSysSchoolDepartment(SysSchoolDepartmentSaveDTO sysSchoolDepartmentSaveDTO) {
        SysSchoolDepartment sysSchoolDepartment = BeanCopyUtils.copy(sysSchoolDepartmentSaveDTO, SysSchoolDepartment.class);
        return schoolDepartmentMapper.insertDepartment(sysSchoolDepartment);
    }

    /**
     * 修改部門
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSysSchoolDepartment(SysSchoolDepartmentSaveDTO sysSchoolDepartmentSaveDTO) {
        SysSchoolDepartment sysSchoolDepartment = BeanCopyUtils.copy(sysSchoolDepartmentSaveDTO, SysSchoolDepartment.class);
        return schoolDepartmentMapper.updateDepartment(sysSchoolDepartment);
    }

    /**
     * 遞歸收集需要刪除的部門 ID
     */
    private  void collectDepartmentIdsToDelete(Long parentId, List<SysSchoolDepartment> allDepartments,
                                               List<Long> idsToCollect) {
        idsToCollect.add(parentId);
        
        // 找到所有以當前部門爲父部門的子部門
        List<SysSchoolDepartment> children = allDepartments.stream()
                .filter(dept -> dept != null && dept.getParentId() != null)
                .filter(dept -> dept.getParentId().longValue() == parentId)
                .collect(Collectors.toList());
        
        // 遞歸處理子部門
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
     * @param type 部門類型（1 學校部門通訊錄, 2 家校通訊錄）
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
