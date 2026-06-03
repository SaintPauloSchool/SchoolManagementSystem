package com.sms.system.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 重發失敗記錄VO
 */
public class ResendFailRecordVO {
    /** 主鍵ID */
    private Long id;

    /** 用戶ID */
    private String userId;

    /** 用戶類型（1學生 2家長） */
    private String userType;

    /** 關聯學生ID */
    private String studentUserId;

    /** 學生名字 */
    private String studentName;

    /** 關係 */
    private String relation;

    /** 累計失敗次數 */
    private Integer failCount;

    /** 狀態：0-待重發 1-已放棄 */
    private String status;

    /** 第1次失敗原因 */
    private String failReason1;

    /** 第1次失敗詳細信息 */
    private String failMessage1;

    /** 第2次失敗原因 */
    private String failReason2;

    /** 第2次失敗詳細信息 */
    private String failMessage2;

    /** 第3次失敗原因 */
    private String failReason3;

    /** 第3次失敗詳細信息 */
    private String failMessage3;

    /** 首次失敗時間 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 最近更新時間 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(String studentUserId) {
        this.studentUserId = studentUserId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailReason1() {
        return failReason1;
    }

    public void setFailReason1(String failReason1) {
        this.failReason1 = failReason1;
    }

    public String getFailMessage1() {
        return failMessage1;
    }

    public void setFailMessage1(String failMessage1) {
        this.failMessage1 = failMessage1;
    }

    public String getFailReason2() {
        return failReason2;
    }

    public void setFailReason2(String failReason2) {
        this.failReason2 = failReason2;
    }

    public String getFailMessage2() {
        return failMessage2;
    }

    public void setFailMessage2(String failMessage2) {
        this.failMessage2 = failMessage2;
    }

    public String getFailReason3() {
        return failReason3;
    }

    public void setFailReason3(String failReason3) {
        this.failReason3 = failReason3;
    }

    public String getFailMessage3() {
        return failMessage3;
    }

    public void setFailMessage3(String failMessage3) {
        this.failMessage3 = failMessage3;
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
