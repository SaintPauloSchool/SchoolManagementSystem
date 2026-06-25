package com.sms.system.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 學生數據匹配表 sys_student_match
 * <p>僅保存已匹配成功的記錄，未匹配學籍資料來自 student_profiles 庫。</p>
 */
public class SysStudentMatch implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String studentProfileNum;
    private String studentUserIdWecom;
    private String studentNameWecom;
    /** 匹配狀態 (0: 未匹配, 1: 自動匹配, 2: 手動匹配) */
    private String matchStatus;
    /** 企微同步狀態 (0: 未同步, 1: 成功, 2: 失敗) */
    private String syncStatus;
    private String errorMsg;
    private Date createTime;
    private Date updateTime;

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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
