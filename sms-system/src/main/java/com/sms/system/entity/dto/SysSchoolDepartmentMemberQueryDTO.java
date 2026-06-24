package com.sms.system.entity.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 系統學校部門成員查詢請求
 */
public class SysSchoolDepartmentMemberQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Long> departmentIds;

    public List<Long> getDepartmentIds() {
        return departmentIds;
    }

    public void setDepartmentIds(List<Long> departmentIds) {
        this.departmentIds = departmentIds;
    }
}
