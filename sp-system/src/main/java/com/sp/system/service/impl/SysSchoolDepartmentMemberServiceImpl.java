package com.sp.system.service.impl;

import com.sp.system.entity.SysSchoolDepartmentMember;
import com.sp.system.mapper.SysSchoolDepartmentMemberMapper;
import com.sp.system.service.ISysSchoolDepartmentMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

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

}
