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

}
