package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 家校通訊錄學段設置
 */
public class AddressBookSegmentSettingDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 通訊錄使用的學段（type=3）部門 ID */
    private Long segmentDepartmentId;

    public Long getSegmentDepartmentId() {
        return segmentDepartmentId;
    }

    public void setSegmentDepartmentId(Long segmentDepartmentId) {
        this.segmentDepartmentId = segmentDepartmentId;
    }
}
