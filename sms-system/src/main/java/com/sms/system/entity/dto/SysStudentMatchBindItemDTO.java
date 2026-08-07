package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 單條學生匹配綁定項（家長 + 企微學生）
 */
public class SysStudentMatchBindItemDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 家長企微 parent_user_id */
    private String parentUserId;
    /** 企微學生 student_user_id */
    private String studentUserId;

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
}
