package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 更新企微學生本地關係表姓名請求
 */
public class SysWecomStudentNameUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentUserId;
    private String studentName;

    public String getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(String studentUserId) {
        this.studentUserId = studentUserId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
}
