package com.sp.system.mapper;

import com.sp.system.entity.SysSchoolDepartmentMember;
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

}
