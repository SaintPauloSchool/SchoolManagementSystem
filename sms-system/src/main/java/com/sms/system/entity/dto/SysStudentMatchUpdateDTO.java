package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 更新學生匹配記錄請求
 */
public class SysStudentMatchUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String studentProfileNum;
    private String studentUserIdWecom;
    private String studentNameWecom;
    private String matchStatus;
    private String syncStatus;
    private String errorMsg;
    private String updateBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentProfileNum() {
        return studentProfileNum;
    }

    public void setStudentProfileNum(String studentProfileNum) {
        this.studentProfileNum = studentProfileNum;
    }

    public String getStudentUserIdWecom() {
        return studentUserIdWecom;
    }

    public void setStudentUserIdWecom(String studentUserIdWecom) {
        this.studentUserIdWecom = studentUserIdWecom;
    }

    public String getStudentNameWecom() {
        return studentNameWecom;
    }

    public void setStudentNameWecom(String studentNameWecom) {
        this.studentNameWecom = studentNameWecom;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
}
