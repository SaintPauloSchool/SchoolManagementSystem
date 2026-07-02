package com.sms.system.service;

import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.SysDepartment;
import com.sms.system.entity.vo.SysDepartmentVO;

import java.util.List;

/**
 * 部門 Service 接口
 *
 */
public interface ISysDepartmentService {

    /**
     * 獲取家校通訊錄樹（帶家長學生關係），按 sys_config 配置的學段構建。
     *
     * @param openUserId 企業微信 userid（當前登錄用戶）
     * @return 過濾後的帶家長學生關係的樹形結構
     */
    List<SysDepartmentVO> getClassTreeWithParentsByAdmin(String openUserId);

    /**
     * 獲取班級部門 ID
     *
     * @return 班級部門 ID 列表
     */
    List<Long> getClassDepartmentId();

    /**
     * 獲取學段樹（僅到 type=3）
     */
    List<SysDepartmentVO> getSegmentTree();

    /**
     * 同步企業微信家校通訊錄部門與管理員數據
     * @param departmentJson 微信接口返回的部門 JSON 數據
     */
    void syncSchoolDepartmentData(JSONObject departmentJson);
}

