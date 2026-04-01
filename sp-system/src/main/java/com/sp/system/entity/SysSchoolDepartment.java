package com.sp.system.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 系统学校部门对象 sys_school_department
 *
 */
public class SysSchoolDepartment implements Serializable {
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

    /** 类型：1-学校部门通讯录，2-家校通讯录 */
    private Integer type;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 子部门列表（树形结构用） */
    private List<SysSchoolDepartment> children;

    /** 是否为叶子节点（用于树形选择器） */
    private Boolean isLeaf;

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

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
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

    public List<SysSchoolDepartment> getChildren() {
        return children;
    }

    public void setChildren(List<SysSchoolDepartment> children) {
        this.children = children;
    }

    public Boolean getIsLeaf() {
        return isLeaf;
    }

    public void setIsLeaf(Boolean isLeaf) {
        this.isLeaf = isLeaf;
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
            .append("type", getType())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("children", getChildren())
            .append("isLeaf", getIsLeaf())
            .toString();
    }
}
