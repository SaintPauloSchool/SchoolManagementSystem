package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 企微學生候選名單查詢條件
 */
public class SysWecomStudentDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 姓名關鍵字 */
    private String queryName;

    /** 家長手機號關鍵字 */
    private String queryMobile;

    /** 班級關鍵字 */
    private String queryClass;

    /** 姓名繁體（服務層轉換，供 SQL 使用） */
    private String queryNameTraditional;

    /** 姓名簡體（服務層轉換，供 SQL 使用） */
    private String queryNameSimplified;

    public String getQueryName() {
        return queryName;
    }

    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    public String getQueryMobile() {
        return queryMobile;
    }

    public void setQueryMobile(String queryMobile) {
        this.queryMobile = queryMobile;
    }

    public String getQueryClass() {
        return queryClass;
    }

    public void setQueryClass(String queryClass) {
        this.queryClass = queryClass;
    }

    public String getQueryNameTraditional() {
        return queryNameTraditional;
    }

    public void setQueryNameTraditional(String queryNameTraditional) {
        this.queryNameTraditional = queryNameTraditional;
    }

    public String getQueryNameSimplified() {
        return queryNameSimplified;
    }

    public void setQueryNameSimplified(String queryNameSimplified) {
        this.queryNameSimplified = queryNameSimplified;
    }
}
