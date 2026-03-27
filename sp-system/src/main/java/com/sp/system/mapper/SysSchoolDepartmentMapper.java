package com.sp.system.mapper;

import com.sp.system.entity.SysSchoolDepartment;
import com.sp.system.entity.SysSchoolDepartmentMember;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学校部门 Mapper 接口
 *
 */
public interface SysSchoolDepartmentMapper {

    /**
     * 查询所有学校部门（按排序值排序）
     *
     * @return 学校部门集合
     */
    List<SysSchoolDepartment> selectAll();

    /**
     * 查询学校部门成员
     *
     * @param departmentId 部门 ID
     * @return 成员集合
     */
    List<SysSchoolDepartmentMember> selectMembersByDepartmentId(@Param("departmentId") Long departmentId);
}
