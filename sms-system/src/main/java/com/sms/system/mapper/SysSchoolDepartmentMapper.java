package com.sms.system.mapper;

import com.sms.system.entity.SysSchoolDepartment;
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
     * @param type 部门类型（1 学校部门 2 家校通讯录）
     * @return 学校部门集合
     */
    List<SysSchoolDepartment> selectAll(@Param("type") Integer type);

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

    /**
     * 插入部门信息
     *
     * @param department 部门信息
     * @return 结果
     */
    int insertDepartment(SysSchoolDepartment department);

    /**
     * 修改部门信息
     *
     * @param department 部门信息
     * @return 结果
     */
    int updateDepartment(SysSchoolDepartment department);

    /**
     * 根据 ID 查询部门
     *
     * @param id 部门 ID
     * @return 部门信息
     */
    SysSchoolDepartment selectById(Long id);

}
