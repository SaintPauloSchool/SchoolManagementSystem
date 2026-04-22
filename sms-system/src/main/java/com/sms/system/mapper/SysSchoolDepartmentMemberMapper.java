package com.sms.system.mapper;

import com.sms.system.entity.SysSchoolDepartmentMember;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统学校部门成员 Mapper 接口
 *
 */
public interface SysSchoolDepartmentMemberMapper {

    /**
     * 批量查询多个部门的成员
     *
     * @param departmentIds 部门 ID 列表
     * @return 成员集合
     */
    List<SysSchoolDepartmentMember> selectMembersByDepartmentIds(@Param("departmentIds") List<Long> departmentIds);

    /**
     * 根据 ID 批量查询部门成员
     * @param ids
     * @return
     */
    List<SysSchoolDepartmentMember> selectMembersByIds(@Param("ids") List<Long> ids);

    /**
     * 根据 ID 删除部门成员
     *
     * @param id 成员 ID
     * @return 影响行数
     */
    int deleteMemberById(@Param("id") Long id);

    /**
     * 根据部门 ID 删除成员
     *
     * @param departmentId 部门 ID
     * @return 影响行数
     */
    int deleteByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * 批量插入部门成员
     *
     * @param members 成员列表
     * @return 影响行数
     */
    int batchInsertMembers(@Param("members") List<SysSchoolDepartmentMember> members);

}
