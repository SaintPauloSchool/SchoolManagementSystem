package com.sms.system.service.impl;

import com.sms.common.exception.ServiceException;
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
import org.springframework.util.StringUtils;

import java.text.Collator;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系統學校部門 Service 實現類
 *
 */
@Service
public class SysSchoolDepartmentServiceImpl implements ISysSchoolDepartmentService {

    private static final Collator NAME_COLLATOR = Collator.getInstance(Locale.TRADITIONAL_CHINESE);

    static {
        NAME_COLLATOR.setStrength(Collator.PRIMARY);
    }

    @Autowired
    private SysSchoolDepartmentMapper schoolDepartmentMapper;

    @Autowired
    private SysSchoolDepartmentMemberMapper schoolDepartmentMemberMapper;

    @Override
    public List<SysSchoolDepartmentVO> getSysSchoolDepartmentTree(Integer type, String ownerUserid) {
        return BeanCopyUtils.copyTree(
                buildDepartmentTree(type, ownerUserid),
                SysSchoolDepartmentVO.class,
                SysSchoolDepartment::getChildren,
                SysSchoolDepartmentVO::setChildren
        );
    }

    @Override
    public List<SysSchoolDepartmentVO> getSysSchoolDepartmentTreeWithMembers(Integer type, String ownerUserid) {
        List<SysSchoolDepartment> rootNodes = buildDepartmentTree(type, ownerUserid);
        if (rootNodes == null || rootNodes.isEmpty()) {
            return Collections.emptyList();
        }

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

        List<SysSchoolDepartmentMember> members = schoolDepartmentMemberMapper.selectMembersByDepartmentIds(departmentIds);
        if (members == null || members.isEmpty()) {
            return BeanCopyUtils.copyTree(rootNodes, SysSchoolDepartmentVO.class,
                    SysSchoolDepartment::getChildren, SysSchoolDepartmentVO::setChildren);
        }

        Map<Long, List<SysSchoolDepartmentMember>> membersByDeptMap = members.stream()
                .filter(Objects::nonNull)
                .filter(m -> m.getSchoolDepartmentId() != null)
                .collect(Collectors.groupingBy(SysSchoolDepartmentMember::getSchoolDepartmentId));

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

        sortDepartmentTreeByName(rootNodes);

        return BeanCopyUtils.copyTree(rootNodes, SysSchoolDepartmentVO.class,
                SysSchoolDepartment::getChildren, SysSchoolDepartmentVO::setChildren);
    }

    private void collectAllDepartments(List<SysSchoolDepartment> nodes, List<SysSchoolDepartment> allDepartments) {
        if (nodes == null) {
            return;
        }
        for (SysSchoolDepartment node : nodes) {
            if (node != null) {
                allDepartments.add(node);
                collectAllDepartments(node.getChildren(), allDepartments);
            }
        }
    }

    private SysSchoolDepartment convertMemberToNode(SysSchoolDepartmentMember member) {
        SysSchoolDepartment node = new SysSchoolDepartment();
        node.setId(-member.getId());
        node.setName(member.getName());
        node.setIsLeaf(true);
        node.setClassDepartmentId(member.getDepartmentId());
        node.setStudentId(member.getStudentId());
        node.setParentUserId(member.getUserid());
        return node;
    }

    private void sortDepartmentTreeByName(List<SysSchoolDepartment> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        nodes.sort(Comparator.comparing(
                dept -> dept.getName() != null ? dept.getName() : "",
                NAME_COLLATOR
        ));
        for (SysSchoolDepartment node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortDepartmentTreeByName(node.getChildren());
            }
        }
    }

    private List<SysSchoolDepartment> buildDepartmentTree(Integer type, String ownerUserid) {
        if (!StringUtils.hasText(ownerUserid)) {
            return Collections.emptyList();
        }
        List<SysSchoolDepartment> allDepartments = schoolDepartmentMapper.selectAll(type, ownerUserid.trim());
        if (allDepartments == null || allDepartments.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<SysSchoolDepartment>> childrenMap = buildChildrenMap(allDepartments);
        List<SysSchoolDepartment> rootNodes = getRootNodes(allDepartments);
        buildTree(rootNodes, childrenMap);
        return rootNodes;
    }

    private Map<Long, List<SysSchoolDepartment>> buildChildrenMap(List<SysSchoolDepartment> allDepartments) {
        return allDepartments.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        dept -> Optional.ofNullable(dept.getParentId()).orElse(0).longValue()
                ));
    }

    private List<SysSchoolDepartment> getRootNodes(List<SysSchoolDepartment> allDepartments) {
        return allDepartments.stream()
                .filter(dept -> Optional.ofNullable(dept.getParentId()).orElse(0) == 0)
                .collect(Collectors.toList());
    }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSysSchoolDepartmentById(Long id, String ownerUserid) {
        assertOwnedBy(id, ownerUserid);

        SysSchoolDepartment targetDept = schoolDepartmentMapper.selectById(id);
        if (targetDept == null) {
            return 0;
        }

        List<SysSchoolDepartment> allDepartments = schoolDepartmentMapper.selectAll(
                targetDept.getType(), ownerUserid.trim());
        if (allDepartments == null || allDepartments.isEmpty()) {
            return 0;
        }

        List<Long> departmentIdsToDelete = new ArrayList<>();
        collectDepartmentIdsToDelete(id, allDepartments, departmentIdsToDelete);
        if (departmentIdsToDelete.isEmpty()) {
            return 0;
        }

        int result = schoolDepartmentMapper.deleteByIds(departmentIdsToDelete.toArray(new Long[0]));
        for (Long deptId : departmentIdsToDelete) {
            schoolDepartmentMemberMapper.deleteByDepartmentId(deptId);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertSysSchoolDepartment(SysSchoolDepartmentSaveDTO sysSchoolDepartmentSaveDTO, String ownerUserid) {
        if (!StringUtils.hasText(ownerUserid)) {
            throw new ServiceException("無法識別當前用戶，請重新登入");
        }
        if (sysSchoolDepartmentSaveDTO == null
                || !StringUtils.hasText(sysSchoolDepartmentSaveDTO.getName())) {
            throw new ServiceException("部門名稱不能為空");
        }

        Integer parentId = sysSchoolDepartmentSaveDTO.getParentId();
        if (parentId != null && parentId > 0) {
            assertOwnedBy(parentId.longValue(), ownerUserid);
        }

        LocalDateTime now = LocalDateTime.now();
        SysSchoolDepartment sysSchoolDepartment = BeanCopyUtils.copy(sysSchoolDepartmentSaveDTO, SysSchoolDepartment.class);
        sysSchoolDepartment.setOwnerUserid(ownerUserid.trim());
        sysSchoolDepartment.setCreateTime(now);
        sysSchoolDepartment.setUpdateTime(now);
        return schoolDepartmentMapper.insertDepartment(sysSchoolDepartment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSysSchoolDepartment(SysSchoolDepartmentSaveDTO sysSchoolDepartmentSaveDTO, String ownerUserid) {
        if (sysSchoolDepartmentSaveDTO == null || sysSchoolDepartmentSaveDTO.getId() == null) {
            throw new ServiceException("部門 ID 不能為空");
        }
        assertOwnedBy(sysSchoolDepartmentSaveDTO.getId(), ownerUserid);

        Integer parentId = sysSchoolDepartmentSaveDTO.getParentId();
        if (parentId != null && parentId > 0) {
            assertOwnedBy(parentId.longValue(), ownerUserid);
        }

        SysSchoolDepartment sysSchoolDepartment = BeanCopyUtils.copy(sysSchoolDepartmentSaveDTO, SysSchoolDepartment.class);
        sysSchoolDepartment.setUpdateTime(LocalDateTime.now());
        return schoolDepartmentMapper.updateDepartment(sysSchoolDepartment);
    }

    @Override
    public void assertOwnedBy(Long departmentId, String ownerUserid) {
        if (departmentId == null) {
            throw new ServiceException("部門 ID 不能為空");
        }
        if (!StringUtils.hasText(ownerUserid)) {
            throw new ServiceException("無法識別當前用戶，請重新登入");
        }
        SysSchoolDepartment dept = schoolDepartmentMapper.selectById(departmentId);
        if (dept == null) {
            throw new ServiceException("部門不存在或已被刪除");
        }
        if (!StringUtils.hasText(dept.getOwnerUserid())
                || !ownerUserid.trim().equals(dept.getOwnerUserid().trim())) {
            throw new ServiceException("無權操作此部門，僅擁有者可管理");
        }
    }

    private void collectDepartmentIdsToDelete(Long parentId, List<SysSchoolDepartment> allDepartments,
                                              List<Long> idsToCollect) {
        idsToCollect.add(parentId);
        List<SysSchoolDepartment> children = allDepartments.stream()
                .filter(dept -> dept != null && dept.getParentId() != null)
                .filter(dept -> dept.getParentId().longValue() == parentId)
                .collect(Collectors.toList());
        for (SysSchoolDepartment child : children) {
            if (child.getId() != null) {
                collectDepartmentIdsToDelete(child.getId(), allDepartments, idsToCollect);
            }
        }
    }
}
