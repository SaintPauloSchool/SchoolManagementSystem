package com.sms.system.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系統學校部門成員對象 sys_school_department_member
 *
 */
public class SysSchoolDepartmentMember implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主鍵 ID */
    private Long id;

    /** 成員 UserID */
    private String userid;

    /** 成員名稱 */
    private String name;

    /** 部門 ID */
    private Long departmentId;

    /** 全局唯一 UserID */
    private String openUserid;

    /** 類型：1-學校部門通訊錄，2-家校通訊錄 */
    private Integer type;

    /** 創建時間 */
    private LocalDateTime createTime;

    /** 更新時間 */
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getOpenUserid() {
        return openUserid;
    }

    public void setOpenUserid(String openUserid) {
        this.openUserid = openUserid;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userid", getUserid())
            .append("name", getName())
            .append("departmentId", getDepartmentId())
            .append("openUserid", getOpenUserid())
            .append("type", getType())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
