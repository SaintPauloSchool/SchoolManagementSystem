package com.sms.system.entity.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 批量手動綁定學生匹配請求
 */
public class SysStudentMatchBatchBindDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 學籍 student_id */
    private String studentId;
    /** 綁定項列表（家長 parent_user_id + 企微學生 student_user_id） */
    private List<SysStudentMatchBindItemDTO> bindings;

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public List<SysStudentMatchBindItemDTO> getBindings() {
        return bindings;
    }

    public void setBindings(List<SysStudentMatchBindItemDTO> bindings) {
        this.bindings = bindings;
    }
}
