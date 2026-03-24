package com.sp.common.core.domain.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.List;

/**
 * 部门对象 sys_department
 *
 */
public class SysDepartment implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 部门 id */
    private Long id;

    /** 父部门 id */
    private Integer parentId;

    /** 部门名称 */
    private String name;

    /** 部门类型：1-班级，2-年级，3-学段，4-校区，5-学校 */
    private Integer type;

    /** 入学年份 */
    private Integer registerYear;

    /** 标准年级 */
    private Integer standardGrade;

    /** 排序值 */
    private Integer orderNum;

    /** 是否毕业：1-是，0-否 */
    private Boolean isGraduated;

    /** 是否开启班级群：1-是，0-否 */
    private Boolean openGroupChat;

    /** 班级群 id */
    private String groupChatId;

    /** 子部门（非数据库字段） */
    private List<SysDepartment> children;

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

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getRegisterYear() {
        return registerYear;
    }

    public void setRegisterYear(Integer registerYear) {
        this.registerYear = registerYear;
    }

    public Integer getStandardGrade() {
        return standardGrade;
    }

    public void setStandardGrade(Integer standardGrade) {
        this.standardGrade = standardGrade;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public Boolean getIsGraduated() {
        return isGraduated;
    }

    public void setIsGraduated(Boolean isGraduated) {
        this.isGraduated = isGraduated;
    }

    public Boolean getOpenGroupChat() {
        return openGroupChat;
    }

    public void setOpenGroupChat(Boolean openGroupChat) {
        this.openGroupChat = openGroupChat;
    }

    public String getGroupChatId() {
        return groupChatId;
    }

    public void setGroupChatId(String groupChatId) {
        this.groupChatId = groupChatId;
    }

    public List<SysDepartment> getChildren() {
        return children;
    }

    public void setChildren(List<SysDepartment> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("parentId", getParentId())
            .append("name", getName())
            .append("type", getType())
            .append("registerYear", getRegisterYear())
            .append("standardGrade", getStandardGrade())
            .append("orderNum", getOrderNum())
            .append("isGraduated", getIsGraduated())
            .append("openGroupChat", getOpenGroupChat())
            .append("groupChatId", getGroupChatId())
            .toString();
    }
}
