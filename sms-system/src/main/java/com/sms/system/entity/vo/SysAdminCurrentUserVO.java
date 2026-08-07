package com.sms.system.entity.vo;

import java.io.Serializable;

/**
 * 當前登入用戶管理員權限 VO
 */
public class SysAdminCurrentUserVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean isAdmin;
    private Boolean isSuperAdmin;
    /** 類型（0超級管理員 1管理員） */
    private String type;
    private String adminName;

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public Boolean getIsSuperAdmin() {
        return isSuperAdmin;
    }

    public void setIsSuperAdmin(Boolean isSuperAdmin) {
        this.isSuperAdmin = isSuperAdmin;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }
}
