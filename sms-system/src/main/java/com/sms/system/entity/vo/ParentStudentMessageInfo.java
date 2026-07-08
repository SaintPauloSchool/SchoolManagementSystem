package com.sms.system.entity.vo;

import java.io.Serializable;

/**
 * 家長-學生消息信息 VO
 * 用於存儲發送給家長的個性化消息所需的完整信息
 */
public class ParentStudentMessageInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 家長用戶 ID */
    private String parentUserId;

    /** 學籍 student_id（用於通知詳情連結 sid 加密） */
    private String studentId;

    /** 班級名稱（SP 格式，如：K1A） */
    private String className;

    /** 學生姓名 */
    private String studentName;

    public ParentStudentMessageInfo() {
    }

    public ParentStudentMessageInfo(String parentUserId, String studentId, String className, String studentName) {
        this.parentUserId = parentUserId;
        this.studentId = studentId;
        this.className = className;
        this.studentName = studentName;
    }

    public String getParentUserId() {
        return parentUserId;
    }

    public void setParentUserId(String parentUserId) {
        this.parentUserId = parentUserId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
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
                ", studentId='" + studentId + '\'' +
                ", className='" + className + '\'' +
                ", studentName='" + studentName + '\'' +
                '}';
    }
}
