package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 學生數據匹配列表查詢條件
 */
public class SysStudentMatchDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 姓名（對應 student_info.id_name / 企微姓名） */
    private String idNameQuery;

    /** 班級（對應 student_info.class_section) */
    private String classSectionQuery;

    /** 匹配狀態 */
    private String matchStatus;

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

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }
}
