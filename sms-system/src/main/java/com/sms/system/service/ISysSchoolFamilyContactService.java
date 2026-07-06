package com.sms.system.service;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * 家校通訊錄聯絡人 Service 接口
 */
public interface ISysSchoolFamilyContactService {

    /**
     * 同步指定班級部門的企微家校通訊錄聯絡人數據。
     *
     * @param targetDepartmentId 目標班級部門 ID（type=1）
     * @param parentJson         企微 {@code getSchoolParentList} 接口返回的 JSON
     */
    void syncSchoolFamilyContactData(Long targetDepartmentId, JSONObject parentJson);

    /**
     * 查詢基本設置所配置學段下、用於每日學生手冊通知發送的家長 userid 列表。
     *
     * @return 去重後的家長企微 userid 列表；未配置學段或無聯絡人時返回空列表
     */
    List<String> getAllParentUserIds();
}
