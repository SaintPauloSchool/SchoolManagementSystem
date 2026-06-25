package com.sms.system.entity.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 部門 VO
 */
public class SysDepartmentVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer parentId;
    private String name;
    private Integer type;
    private Integer registerYear;
    private Integer standardGrade;
    private Integer orderNum;
    private Boolean isGraduated;
    private Boolean openGroupChat;
    private String groupChatId;
    private String parentUserId;
    private String studentUserId;
    private String relationDesc;
    private String mobile;
    private Boolean isLeaf;
    private Long classDepartmentId;
    private List<SysDepartmentVO> children;

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

    public String getParentUserId() {
        return parentUserId;
    }

    public void setParentUserId(String parentUserId) {
        this.parentUserId = parentUserId;
    }

    public String getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(String studentUserId) {
        this.studentUserId = studentUserId;
    }

    public String getRelationDesc() {
        return relationDesc;
    }

    public void setRelationDesc(String relationDesc) {
        this.relationDesc = relationDesc;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
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

    public List<SysDepartmentVO> getChildren() {
        return children;
    }

    public void setChildren(List<SysDepartmentVO> children) {
        this.children = children;
    }
}
