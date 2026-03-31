package com.sp.system.mapper;

import com.sp.system.entity.WecomSchoolDepartment;
import java.util.List;

/**
 * wecom学校部门 Mapper 接口
 *
 */
public interface WecomSchoolDepartmentMapper {

    /**
     * 查询所有学校部门（按排序值排序）
     *
     * @return 学校部门集合
     */
    List<WecomSchoolDepartment> selectAll();

}
