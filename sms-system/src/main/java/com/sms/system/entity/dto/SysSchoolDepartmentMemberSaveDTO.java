package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 系統學校部門成員批量新增單條請求
 */
public class SysSchoolDepartmentMemberSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userid;
    private String name;
    private Long departmentId;
    private String openUserid;
    /** 學籍 student_id（家校通訊錄成員必填） */
    private String studentId;

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getOpenUserid() {
        return openUserid;
    }

    public void setOpenUserid(String openUserid) {
        this.openUserid = openUserid;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}
