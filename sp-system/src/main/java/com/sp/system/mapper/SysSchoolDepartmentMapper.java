package com.sp.system.mapper;

import com.sp.system.entity.SysSchoolDepartment;

import java.util.List;

/**
 * 系统学校部门 Mapper 接口
 *
 */
public interface SysSchoolDepartmentMapper {

    /**
     * 查询所有学校部门（按排序值排序）
     *
     * @return 学校部门集合
     */
    List<SysSchoolDepartment> selectAll();

}
