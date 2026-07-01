package com.sms.system.service;

/**
 * 系統公用配置 Service
 */
public interface ISysConfigService {

    /** 讀取家校通訊錄使用的學段部門 ID */
    Long getAddressBookSegmentDepartmentId();

    /** 保存家校通訊錄使用的學段部門 ID */
    void saveAddressBookSegmentDepartmentId(Long segmentDepartmentId, String updateBy);
}
