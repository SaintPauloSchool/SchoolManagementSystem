package com.sms.system.service;

import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.WecomSchoolDepartment;

import java.util.List;
import java.util.Map;

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

    /**
     * 同步企業微信部門資料
     * @param result 微信接口返回的部門數據
     */
    void syncWecomSchoolDepartments(JSONObject result);

    /**
     * 批次同步企業微信多個部門的成員資料（優化效能，避免 N+1 查詢）
     * @param departmentMembersMap 部門 ID 對應的成員結果 Map
     */
    void syncWecomSchoolDepartmentMembersBatch(Map<Long, JSONObject> departmentMembersMap);
}
