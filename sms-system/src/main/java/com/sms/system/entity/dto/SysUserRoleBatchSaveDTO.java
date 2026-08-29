package com.sms.system.entity.dto;

import java.util.List;

/**
 * 批量新增用戶角色
 */
public class SysUserRoleBatchSaveDTO {

    private List<SysUserRoleItem> userRoles;
    /** 默認類型（0超級管理員 1管理員 2其他） */
    private String type;
    private String senderDisplayName;
    private String remark;

    public List<SysUserRoleItem> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(List<SysUserRoleItem> userRoles) {
        this.userRoles = userRoles;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public static class SysUserRoleItem {
        private String userId;
        private String userName;
        private String type;
        private String senderDisplayName;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getSenderDisplayName() {
            return senderDisplayName;
        }

        public void setSenderDisplayName(String senderDisplayName) {
            this.senderDisplayName = senderDisplayName;
        }
    }
}
