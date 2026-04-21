package com.sms.system.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * wecom学校部门对象 wecom_school_department
 *
 */
public class WecomSchoolDepartment implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 部门 id */
    private Long id;

    /** 父部门 id */
    private Integer parentId;

    /** 部门名称 */
    private String name;

    /** 部门英文名称 */
    private String nameEn;

    /** 在父部门中的次序值 */
    private Integer orderNum;

    /** 部门负责人的 UserID（JSON 数组字符串） */
    private String departmentLeader;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 子部门/成员列表（树形结构用） */
    private List<WecomSchoolDepartment> children;

    /** 是否为叶子节点（用于树形选择器） */
    private Boolean isLeaf;

    /** 教职员工 UserID（用于前端识别） */
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
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
