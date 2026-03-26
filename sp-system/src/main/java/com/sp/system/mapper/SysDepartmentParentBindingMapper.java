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
     * 根据部门 ID 查询家长绑定列表
     *
     * @param departmentId 部门 ID
     * @return 家长绑定集合
     */
    List<SysDepartmentParentBinding> selectByDepartmentId(@Param("departmentId") Long departmentId);

}
