package com.sp.system.service;

import com.sp.system.entity.SysSchoolDepartmentMember;

import java.util.List;

/**
 * 系统学校部门成员 Service 接口
 *
 */
public interface ISysSchoolDepartmentMemberService {

    /**
     * 批量查询多个部门的成员列表
     *
     * @param departmentIds 部门 ID 列表
     * @return 成员列表
     */
    List<SysSchoolDepartmentMember> getMembersByDepartmentIds(List<Long> departmentIds);

    /**
     * 根据 ID 删除部门成员
     *
     * @param id 成员 ID
     * @return 影响行数
     */
    int deleteMemberById(Long id);

}
