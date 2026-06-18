package com.sms.system.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 系統學校部門對象 sys_school_department
 *
 */
public class SysSchoolDepartment implements Serializable {
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

    /** 類型：1-學校部門通訊錄，2-家校通訊錄 */
    private Integer type;

    /** 創建時間 */
    private LocalDateTime createTime;

    /** 更新時間 */
    private LocalDateTime updateTime;

    /** 子部門列表（樹形結構用） */
    private List<SysSchoolDepartment> children;

    /** 是否爲葉子節點（用於樹形選擇器） */
    private Boolean isLeaf;

    /** 葉子節點所屬部門 ID（成員節點使用，非數據庫字段） */
    private Long classDepartmentId;

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

    public Long getClassDepartmentId() {
        return classDepartmentId;
    }

    public void setClassDepartmentId(Long classDepartmentId) {
        this.classDepartmentId = classDepartmentId;
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
