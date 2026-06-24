package com.sms.system.service;

import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.vo.WecomSchoolDepartmentVO;

import java.util.List;
import java.util.Map;

/**
 * wecom學校部門 Service 接口
 *
 */
public interface IWecomSchoolDepartmentService {

    /**
     * 獲取學校部門樹形結構（帶成員，用於教職員工選擇器）
     * 在構建樹形結構的基礎上爲每個部門加載成員數據
     *
     * @return 帶成員的部門樹形結構
     */
    List<WecomSchoolDepartmentVO> getWecomSchoolDepartmentTreeWithMembers();

    /**
     * 獲取學校部門樹形結構（僅部門，不含人員）
     * 只返回部門層級結構，不包含人員數據
     *
     * @return 僅部門的樹形結構
     */
    List<WecomSchoolDepartmentVO> getWecomSchoolDepartmentTree();

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
