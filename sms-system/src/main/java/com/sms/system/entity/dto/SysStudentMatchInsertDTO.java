package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 新增學生匹配記錄請求
 */
public class SysStudentMatchInsertDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentProfileNum;

    public String getStudentProfileNum() {
        return studentProfileNum;
    }

    public void setStudentProfileNum(String studentProfileNum) {
        this.studentProfileNum = studentProfileNum;
    }
}
