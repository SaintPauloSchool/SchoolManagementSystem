package com.sms.system.entity.vo;

import java.io.Serializable;

/**
 * 當前登入用戶角色 VO
 */
public class SysUserRoleCurrentUserVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean hasUserRole;
    private Boolean hasSuperUserRole;
    /** 類型（0超級管理員 1管理員 2其他） */
    private String type;
    private String userName;
    private String senderDisplayName;
    /** 解析後的發送人顯示名（角色名或登錄名） */
    private String senderName;

    public Boolean getHasUserRole() {
        return hasUserRole;
    }

    public void setHasUserRole(Boolean hasUserRole) {
        this.hasUserRole = hasUserRole;
    }

    public Boolean getHasSuperUserRole() {
        return hasSuperUserRole;
    }

    public void setHasSuperUserRole(Boolean hasSuperUserRole) {
        this.hasSuperUserRole = hasSuperUserRole;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
}
