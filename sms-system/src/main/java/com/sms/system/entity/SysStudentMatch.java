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
    /** 學生 ID（關聯 student_profiles.student_info.student_id） */
    private String studentId;
    /** 家校通訊錄家長 user_id（parent_user_id） */
    private String userId;
    /** 匹配狀態 (0: 未匹配, 1: 自動匹配, 2: 手動匹配) */
    private String matchStatus;
    private Date createTime;
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
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
