package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 手動綁定學生匹配請求
 * <p>綁定前尚無 {@code sys_student_match} 記錄，僅需學籍 {@code studentId} 與家長 {@code userId}。</p>
 */
public class SysStudentMatchBindDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 學籍 student_id */
    private String studentId;
    /** 家長企微 parent_user_id */
    private String userId;

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
