package com.sms.system.entity.vo;

import java.io.Serializable;

/**
 * 家长-学生消息信息 VO
 * 用于存储发送给家长的个性化消息所需的完整信息
 */
public class ParentStudentMessageInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 家长用户 ID */
    private String parentUserId;

    /** 学生用户 ID */
    private String studentUserId;

    /** 班级名称（SP 格式，如：K1A） */
    private String className;

    /** 学生姓名 */
    private String studentName;

    public ParentStudentMessageInfo() {
    }

    public ParentStudentMessageInfo(String parentUserId, String studentUserId, String className, String studentName) {
        this.parentUserId = parentUserId;
        this.studentUserId = studentUserId;
        this.className = className;
        this.studentName = studentName;
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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    @Override
    public String toString() {
        return "ParentStudentMessageInfo{" +
                "parentUserId='" + parentUserId + '\'' +
                ", studentUserId='" + studentUserId + '\'' +
                ", className='" + className + '\'' +
                ", studentName='" + studentName + '\'' +
                '}';
    }
}
