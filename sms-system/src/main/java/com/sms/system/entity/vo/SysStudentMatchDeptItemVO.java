package com.sms.system.entity.vo;

import java.io.Serializable;

/**
 * 學生與企微班級部門綁定項
 */
public class SysStudentMatchDeptItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentUserId;
    private Long departmentId;

    public String getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(String studentUserId) {
        this.studentUserId = studentUserId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }
}
