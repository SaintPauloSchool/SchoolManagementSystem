package com.sms.system.entity.dto;

/**
 * 系統管理員查詢條件
 */
public class SysAdminQueryDTO {

    /** 管理員姓名（模糊） */
    private String adminName;

    /** 用戶ID（模糊） */
    private String userId;

    /** 類型（0超級管理員 1管理員） */
    private String type;

    /** 狀態（0正常 1停用） */
    private String status;

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
