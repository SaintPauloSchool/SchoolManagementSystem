package com.sms.system.entity.dto;

/**
 * 系統用戶角色查詢條件
 */
public class SysUserRoleQueryDTO {

    private String userName;
    private String userId;
    /** 類型（0超級管理員 1管理員 2其他） */
    private String type;
    private String status;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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
