package com.sms.system.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 學生數據匹配表 sys_student_match
 * <p>學生與家長為多對多：同一 student_id 可對應多條記錄（每位家長聯絡人一條，以 parent_user_id + student_user_id 唯一定位）。</p>
 */
public class SysStudentMatch implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    /** 學生 ID（關聯 student_profiles.student_info.student_id） */
    private String studentId;
    /** 家校通訊錄家長 user_id（parent_user_id） */
    private String userId;
    /** 家校通訊錄學生 user_id（關聯 sys_school_family_contact.student_user_id） */
    private String studentUserId;
    private Integer matchStatus;
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

    public String getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(String studentUserId) {
        this.studentUserId = studentUserId;
    }

    public Integer getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(Integer matchStatus) {
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
