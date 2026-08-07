package com.sms.system.entity.dto;

/**
 * 修改管理員
 */
public class SysAdminUpdateDTO {

    private Long id;

    /** 類型（0超級管理員 1管理員） */
    private String type;

    /** 狀態（0正常 1停用） */
    private String status;

    /** 管理員姓名 */
    private String adminName;

    /** 備註 */
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
