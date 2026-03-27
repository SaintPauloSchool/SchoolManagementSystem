package com.sp.system.mapper;

import com.sp.system.entity.SysDepartmentParentBinding;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门家长绑定 Mapper 接口
 *
 */
public interface SysDepartmentParentBindingMapper {

    /**
     * 根据部门 ID 列表批量查询家长绑定列表
     *
     * @param departmentIds 部门 ID 列表
     * @return 家长绑定集合
     */
    List<SysDepartmentParentBinding> selectByDepartmentIds(@Param("departmentIds") List<Long> departmentIds);

}
