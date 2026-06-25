package com.sms.system.entity.vo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * WeCom 學校部門 VO
 */
public class WecomSchoolDepartmentVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer parentId;
    private String name;
    private String nameEn;
    private Integer orderNum;
    private String departmentLeader;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<WecomSchoolDepartmentVO> children;
    private Boolean isLeaf;
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

    public List<WecomSchoolDepartmentVO> getChildren() {
        return children;
    }

    public void setChildren(List<WecomSchoolDepartmentVO> children) {
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
}
