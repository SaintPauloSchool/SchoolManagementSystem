package com.sms.system.entity.vo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 系統學校部門 VO
 */
public class SysSchoolDepartmentVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer parentId;
    private String name;
    private String nameEn;
    private Integer orderNum;
    private String departmentLeader;
    private Integer type;
    private String ownerUserid;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<SysSchoolDepartmentVO> children;
    private Boolean isLeaf;
    private Long classDepartmentId;
    private String studentId;
    private String parentUserId;

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

    public String getOwnerUserid() {
        return ownerUserid;
    }

    public void setOwnerUserid(String ownerUserid) {
        this.ownerUserid = ownerUserid;
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

    public List<SysSchoolDepartmentVO> getChildren() {
        return children;
    }

    public void setChildren(List<SysSchoolDepartmentVO> children) {
        this.children = children;
    }

    public Boolean getIsLeaf() {
        return isLeaf;
    }

    public void setIsLeaf(Boolean isLeaf) {
        this.isLeaf = isLeaf;
    }

    public Long getClassDepartmentId() {
        return classDepartmentId;
    }

    public void setClassDepartmentId(Long classDepartmentId) {
        this.classDepartmentId = classDepartmentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getParentUserId() {
        return parentUserId;
    }

    public void setParentUserId(String parentUserId) {
        this.parentUserId = parentUserId;
    }
}
