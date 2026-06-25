package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 依學生個人編號查詢匹配記錄請求
 */
public class SysStudentMatchProfileNumDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentProfileNum;

    public SysStudentMatchProfileNumDTO() {
    }

    public SysStudentMatchProfileNumDTO(String studentProfileNum) {
        this.studentProfileNum = studentProfileNum;
    }

    public String getStudentProfileNum() {
        return studentProfileNum;
    }

    public void setStudentProfileNum(String studentProfileNum) {
        this.studentProfileNum = studentProfileNum;
    }
}
