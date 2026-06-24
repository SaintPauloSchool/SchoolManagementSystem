package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 系統學校部門樹查詢請求
 */
public class SysSchoolDepartmentQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 類型：1-學校部門通訊錄，2-家校通訊錄 */
    private Integer type;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
