package com.sms.system.entity.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 查詢學生企微班級部門 ID 請求
 */
public class SysStudentMatchDeptQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<String> studentUserIds;

    public List<String> getStudentUserIds() {
        return studentUserIds;
    }

    public void setStudentUserIds(List<String> studentUserIds) {
        this.studentUserIds = studentUserIds;
    }
}
