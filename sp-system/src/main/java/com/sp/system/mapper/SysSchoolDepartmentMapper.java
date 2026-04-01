package com.sp.system.mapper;

import com.sp.system.entity.SysSchoolDepartment;
import org.apache.ibatis.annotations.Param;

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

    /**
     * 根据 ID 删除学校部门
     *
     * @param id 部门 ID
     * @return 结果
     */
    int deleteById(Long id);

    /**
     * 根据 ID 批量删除学校部门
     *
     * @param ids 部门 ID 数组
     * @return 结果
     */
    int deleteByIds(@Param("ids") Long[] ids);

}
