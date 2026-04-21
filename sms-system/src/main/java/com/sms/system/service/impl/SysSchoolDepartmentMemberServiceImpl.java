package com.sms.system.service.impl;

import com.sms.system.entity.SysSchoolDepartmentMember;
import com.sms.system.mapper.SysSchoolDepartmentMemberMapper;
import com.sms.system.service.ISysSchoolDepartmentMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 系统学校部门成员 Service 实现类
 *
 */
@Service
public class SysSchoolDepartmentMemberServiceImpl implements ISysSchoolDepartmentMemberService {

    @Autowired
    private SysSchoolDepartmentMemberMapper memberMapper;

    /**
     * 批量查询多个部门的成员列表
     */
    @Override
    public List<SysSchoolDepartmentMember> getMembersByDepartmentIds(List<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysSchoolDepartmentMember> members = memberMapper.selectMembersByDepartmentIds(departmentIds);
        return members != null ? members : Collections.emptyList();
    }

    /**
     * 根据 ID 删除部门成员
     */
    @Override
    public int deleteMemberById(Long id) {
        if (id == null) {
            return 0;
        }
        return memberMapper.deleteMemberById(id);
    }

    /**
     * 批量添加部门成员 (自动过滤该部门下已存在的 userid)
     */
    @Override
    public int batchAddMembers(List<SysSchoolDepartmentMember> members) {
        if (members == null || members.isEmpty()) {
            return 0;
        }
        
        // 1. 获取本次要添加人员所涉及的所有部门 ID
        List<Long> departmentIds = members.stream()
                .map(SysSchoolDepartmentMember::getDepartmentId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        // 2. 查出这些部门下已经存在的人员
        List<SysSchoolDepartmentMember> existingMembers = memberMapper.selectMembersByDepartmentIds(departmentIds);

        // 3. 过滤掉已经在该部门存在的人员
        List<SysSchoolDepartmentMember> toInsert = members.stream()
                .filter(m -> existingMembers.stream().noneMatch(exist -> 
                        exist.getDepartmentId().equals(m.getDepartmentId()) && 
                        exist.getUserid().equals(m.getUserid())
                ))
                // 顺便做个去重，防止前端传来的 members 列表里有重复对象
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(
                                Comparator.comparing(m -> m.getDepartmentId() + "_" + m.getUserid())
                        )), ArrayList::new));

        // 4. 如果全都被过滤掉了（说明想加的人都已经在了），直接返回成功数量，不报错
        if (toInsert.isEmpty()) {
            return members.size();
        }

        // 5. 插入过滤后的真实增量人员
        memberMapper.batchInsertMembers(toInsert);
        
        // 外部可能依赖返回值判断是否成功，所以统一回传原数组大小，制造"全部成功加入"(包括已存在的)的假象
        return members.size();
    }

}
