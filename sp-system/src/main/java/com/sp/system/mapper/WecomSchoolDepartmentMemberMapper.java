package com.sp.system.mapper;

import com.sp.system.entity.WecomSchoolDepartmentMember;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * wecom学校部门成员 Mapper 接口
 *
 */
public interface WecomSchoolDepartmentMemberMapper {

    /**
     * 批量查询多个部门的成员
     *
     * @param departmentIds 部门 ID 列表
     * @return 成员集合
     */
    List<WecomSchoolDepartmentMember> selectMembersByDepartmentIds(@Param("departmentIds") List<Long> departmentIds);
}
