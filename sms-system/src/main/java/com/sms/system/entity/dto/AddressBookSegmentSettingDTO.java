package com.sms.system.entity.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 家校通訊錄學段設置（支援多選）
 */
public class AddressBookSegmentSettingDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 通訊錄使用的學段（type=3）部門 ID 列表 */
    private List<Long> segmentDepartmentIds;

    public List<Long> getSegmentDepartmentIds() {
        return segmentDepartmentIds;
    }

    public void setSegmentDepartmentIds(List<Long> segmentDepartmentIds) {
        this.segmentDepartmentIds = segmentDepartmentIds;
    }
}
