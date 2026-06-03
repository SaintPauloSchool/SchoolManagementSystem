package com.sms.system.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * wecom學校部門對象 wecom_school_department
 *
 */
public class WecomSchoolDepartment implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 部門 id */
    private Long id;

    /** 父部門 id */
    private Integer parentId;

    /** 部門名稱 */
    private String name;

    /** 部門英文名稱 */
    private String nameEn;

    /** 在父部門中的次序值 */
    private Integer orderNum;

    /** 部門負責人的 UserID（JSON 數組字符串） */
    private String departmentLeader;

    /** 創建時間 */
    private LocalDateTime createTime;

    /** 更新時間 */
    private LocalDateTime updateTime;

    /** 子部門/成員列表（樹形結構用） */
    private List<WecomSchoolDepartment> children;

    /** 是否爲葉子節點（用於樹形選擇器） */
    private Boolean isLeaf;

    /** 教職員工 UserID（用於前端識別） */
    private String staffUserId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public String getDepartmentLeader() {
        return departmentLeader;
    }

    public void setDepartmentLeader(String departmentLeader) {
        this.departmentLeader = departmentLeader;
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

    public java.util.List<WecomSchoolDepartment> getChildren() {
        return children;
    }

    public void setChildren(java.util.List<WecomSchoolDepartment> children) {
        this.children = children;
    }

    public Boolean getIsLeaf() {
        return isLeaf;
    }

    public void setIsLeaf(Boolean isLeaf) {
        this.isLeaf = isLeaf;
    }

    public String getStaffUserId() {
        return staffUserId;
    }

    public void setStaffUserId(String staffUserId) {
        this.staffUserId = staffUserId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("parentId", getParentId())
            .append("name", getName())
            .append("nameEn", getNameEn())
            .append("orderNum", getOrderNum())
            .append("departmentLeader", getDepartmentLeader())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("children", getChildren())
            .append("isLeaf", getIsLeaf())
            .append("staffUserId", getStaffUserId())
            .toString();
    }
}
