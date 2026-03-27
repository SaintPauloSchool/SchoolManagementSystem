package com.sp.system.service;

import com.sp.system.entity.SysSchoolDepartment;

import java.util.List;

/**
 * 学校部门 Service 接口
 *
 */
public interface ISysSchoolDepartmentService {

    /**
     * 获取学校部门树形结构（带成员，用于教职员工选择器）
     * 在构建树形结构的基础上为每个部门加载成员数据
     *
     * @return 带成员的部门树形结构
     */
    List<SysSchoolDepartment> getSchoolDepartmentTreeWithMembers();
}
