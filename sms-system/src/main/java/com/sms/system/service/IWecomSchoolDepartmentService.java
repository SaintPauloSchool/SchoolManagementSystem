package com.sms.system.service;

import com.sms.system.entity.WecomSchoolDepartment;

import java.util.List;

/**
 * wecom学校部门 Service 接口
 *
 */
public interface IWecomSchoolDepartmentService {

    /**
     * 获取学校部门树形结构（带成员，用于教职员工选择器）
     * 在构建树形结构的基础上为每个部门加载成员数据
     *
     * @return 带成员的部门树形结构
     */
    List<WecomSchoolDepartment> getWecomSchoolDepartmentTreeWithMembers();

    /**
     * 获取学校部门树形结构（仅部门，不含人员）
     * 只返回部门层级结构，不包含人员数据
     *
     * @return 仅部门的树形结构
     */
    List<WecomSchoolDepartment> getWecomSchoolDepartmentTree();
}
