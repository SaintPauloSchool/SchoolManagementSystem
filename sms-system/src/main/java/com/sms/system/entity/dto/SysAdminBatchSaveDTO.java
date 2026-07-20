package com.sms.system.entity.dto;

import java.util.List;

/**
 * 批量新增管理員（來自 WeCom 老師通訊錄選人）
 */
public class SysAdminBatchSaveDTO {

    /** 待新增的管理員列表 */
    private List<SysAdminItem> admins;

    /** 默認類型（0超級管理員 1管理員），未單獨指定時使用 */
    private String type;

    /** 備註 */
    private String remark;

    public List<SysAdminItem> getAdmins() {
        return admins;
    }

    public void setAdmins(List<SysAdminItem> admins) {
        this.admins = admins;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public static class SysAdminItem {
        /** WeCom userid（寫入 sys_admin.user_id） */
        private String userId;
        /** 顯示姓名 */
        private String adminName;
        /** 可選：單獨指定類型 */
        private String type;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getAdminName() {
            return adminName;
        }

        public void setAdminName(String adminName) {
            this.adminName = adminName;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
