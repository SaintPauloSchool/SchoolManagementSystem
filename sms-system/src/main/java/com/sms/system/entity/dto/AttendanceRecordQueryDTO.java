package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 考勤機記錄查詢條件
 */
public class AttendanceRecordQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 學生姓名（對應 student_info.id_name） */
    private String idNameQuery;

    /** 班級（對應 student_info.class_section） */
    private String classSectionQuery;

    /** 班號（對應 student_info.class_num） */
    private String classNumQuery;

    /** 學生 ID（對應 attendance_record.employee_id） */
    private String employeeIdQuery;

    /** 拍卡日期起（yyyy-MM-dd） */
    private String accessDateBegin;

    /** 拍卡日期止（yyyy-MM-dd） */
    private String accessDateEnd;

    public String getIdNameQuery() {
        return idNameQuery;
    }

    public void setIdNameQuery(String idNameQuery) {
        this.idNameQuery = idNameQuery;
    }

    public String getClassSectionQuery() {
        return classSectionQuery;
    }

    public void setClassSectionQuery(String classSectionQuery) {
        this.classSectionQuery = classSectionQuery;
    }

    public String getClassNumQuery() {
        return classNumQuery;
    }

    public void setClassNumQuery(String classNumQuery) {
        this.classNumQuery = classNumQuery;
    }

    public String getEmployeeIdQuery() {
        return employeeIdQuery;
    }

    public void setEmployeeIdQuery(String employeeIdQuery) {
        this.employeeIdQuery = employeeIdQuery;
    }

    public String getAccessDateBegin() {
        return accessDateBegin;
    }

    public void setAccessDateBegin(String accessDateBegin) {
        this.accessDateBegin = accessDateBegin;
    }

    public String getAccessDateEnd() {
        return accessDateEnd;
    }

    public void setAccessDateEnd(String accessDateEnd) {
        this.accessDateEnd = accessDateEnd;
    }
}
