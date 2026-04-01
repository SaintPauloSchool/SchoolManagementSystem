package com.sp.system.service;

import com.sp.system.entity.SysSchoolDepartment;
import com.sp.system.entity.SysSchoolDepartmentMember;

import java.util.List;

/**
 * 系统学校部门 Service 接口
 *
 */
public interface ISysSchoolDepartmentService {

    /**
     * 获取学校部门树形结构（带成员）
     *
     * @return 部门树形结构
     */
    List<SysSchoolDepartment> getSysSchoolDepartmentTreeWithMembers();

    /**
     * 获取学校部门树形结构（仅部门，不含人员）
     *
     * @return 部门树形结构
     */
    List<SysSchoolDepartment> getSysSchoolDepartmentTree();

    /**
     * 根据 ID 查询部门详情
     *
     * @param id 部门 ID
     * @return 部门信息
     */
    SysSchoolDepartment getDepartmentById(Long id);

    /**
     * 新增部门
     *
     * @param department 部门信息
     * @return 影响行数
     */
    int addDepartment(SysSchoolDepartment department);

    /**
     * 修改部门
     *
     * @param department 部门信息
     * @return 影响行数
     */
    int updateDepartment(SysSchoolDepartment department);

    /**
     * 删除部门
     *
     * @param id 部门 ID
     * @return 影响行数
     */
    int deleteDepartmentById(Long id);

    /**
     * 新增部门成员
     *
     * @param member 成员信息
     * @return 影响行数
     */
    int addDepartmentMember(SysSchoolDepartmentMember member);

    /**
     * 修改部门成员
     *
     * @param member 成员信息
     * @return 影响行数
     */
    int updateDepartmentMember(SysSchoolDepartmentMember member);

    /**
     * 删除部门成员
     *
     * @param id 成员 ID
     * @return 影响行数
     */
    int deleteDepartmentMemberById(Long id);
}
