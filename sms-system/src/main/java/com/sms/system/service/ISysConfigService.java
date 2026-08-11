package com.sms.system.service;

import java.util.List;

/**
 * 系統公用配置 Service
 */
public interface ISysConfigService {

    /**
     * 讀取家校通訊錄使用的學段部門 ID 列表（type=3）。
     * <p>兼容舊配置：單個數字會解析為單元素列表。</p>
     */
    List<Long> getAddressBookSegmentDepartmentIds();

    /** 保存家校通訊錄使用的學段部門 ID 列表（type=3，逗號分隔） */
    void saveAddressBookSegmentDepartmentIds(List<Long> segmentDepartmentIds, String updateBy);

    /** 讀取每日學生手冊通知發送範圍的班級部門 ID 列表（type=1） */
    List<Long> getDailyNoticeClassDepartmentIds();

    /** 保存每日學生手冊通知發送範圍的班級部門 ID 列表（type=1） */
    void saveDailyNoticeClassDepartmentIds(List<Long> classDepartmentIds, String updateBy);
}
