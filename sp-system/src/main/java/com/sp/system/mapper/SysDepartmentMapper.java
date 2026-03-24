package com.sp.system.mapper;

import com.sp.common.core.domain.entity.SysDepartment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门 Mapper 接口
 *
 */
public interface SysDepartmentMapper {

    /**
     * 根据类型查询部门列表
     *
     * @param type 部门类型
     * @return 部门集合
     */
    List<SysDepartment> selectByType(@Param("type") Integer type);

    /**
     * 根据父级 ID 查询子部门
     *
     * @param parentId 父级 ID
     * @return 部门集合
     */
    List<SysDepartment> selectByParentId(@Param("parentId") Integer parentId);

    /**
     * 查询所有部门（按类型和排序值排序）
     *
     * @return 部门集合
     */
    List<SysDepartment> selectAll();
}
