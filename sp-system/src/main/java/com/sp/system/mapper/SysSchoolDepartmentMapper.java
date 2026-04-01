package com.sp.system.mapper;

import com.sp.system.entity.SysSchoolDepartment;
import com.sp.system.entity.SysSchoolDepartmentMember;
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
     * 根据 ID 查询部门详情
     *
     * @param id 部门 ID
     * @return 部门信息
     */
    SysSchoolDepartment selectById(@Param("id") Long id);

    /**
     * 插入部门信息
     *
     * @param department 部门信息
     * @return 影响行数
     */
    int insert(SysSchoolDepartment department);

    /**
     * 更新部门信息
     *
     * @param department 部门信息
     * @return 影响行数
     */
    int update(SysSchoolDepartment department);

    /**
     * 删除部门信息
     *
     * @param id 部门 ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 批量查询多个部门的成员
     *
     * @param departmentIds 部门 ID 列表
     * @return 成员集合
     */
    List<SysSchoolDepartmentMember> selectMembersByDepartmentIds(@Param("departmentIds") List<Long> departmentIds);

    /**
     * 插入部门成员信息
     *
     * @param member 成员信息
     * @return 影响行数
     */
    int insertMember(SysSchoolDepartmentMember member);

    /**
     * 更新部门成员信息
     *
     * @param member 成员信息
     * @return 影响行数
     */
    int updateMember(SysSchoolDepartmentMember member);

    /**
     * 删除部门成员信息
     *
     * @param id 成员 ID
     * @return 影响行数
     */
    int deleteMemberById(@Param("id") Long id);

    /**
     * 根据部门 ID 删除所有成员
     *
     * @param departmentId 部门 ID
     * @return 影响行数
     */
    int deleteMembersByDepartmentId(@Param("departmentId") Long departmentId);
}
