package com.sms.system.service;

import com.sms.system.entity.SysSchoolDepartment;

import java.util.List;

/**
 * 系统学校部门 Service 接口
 *
 */
public interface ISysSchoolDepartmentService {

    /**
     * 获取学校部门树形结构（仅部门，不含人员）
     *
     * @param type 部门类型（1 学校部门 2 家校通讯录）
     * @return 部门树形结构
     */
    List<SysSchoolDepartment> getSysSchoolDepartmentTree(Integer type);

    /**
     * 获取学校部门树形结构（包含人员作为叶子节点）
     *
     * @param type 部门类型（1 学校部门 2 家校通讯录）
     * @return 部门树形结构，人员作为负ID的叶节点
     */
    List<SysSchoolDepartment> getSysSchoolDepartmentTreeWithMembers(Integer type);

    /**
     * 根据 ID 删除学校部门
     * 同时删除该部门下的所有子部门和成员
     *
     * @param id 部门 ID
     * @return 结果
     */
    int deleteSysSchoolDepartmentById(Long id);

    /**
     * 新增部门
     *
     * @param department 部门信息
     * @return 结果
     */
    int insertSysSchoolDepartment(SysSchoolDepartment department);

    /**
     * 修改部门
     *
     * @param department 部门信息
     * @return 结果
     */
    int updateSysSchoolDepartment(SysSchoolDepartment department);

    /**
     * 根据 ID 查询部门
     *
     * @param id 部门 ID
     * @return 部门信息
     */
    SysSchoolDepartment selectSysSchoolDepartmentById(Long id);

}
