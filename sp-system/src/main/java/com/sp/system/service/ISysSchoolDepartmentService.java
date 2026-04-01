package com.sp.system.service;

import com.sp.system.entity.SysSchoolDepartment;

import java.util.List;

/**
 * 系统学校部门 Service 接口
 *
 */
public interface ISysSchoolDepartmentService {

    /**
     * 获取学校部门树形结构（仅部门，不含人员）
     *
     * @return 部门树形结构
     */
    List<SysSchoolDepartment> getSysSchoolDepartmentTree();

    /**
     * 根据 ID 删除学校部门
     * 同时删除该部门下的所有子部门和成员
     *
     * @param id 部门 ID
     * @return 结果
     */
    int deleteSysSchoolDepartmentById(Long id);

}
