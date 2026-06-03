package com.sms.system.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 失敗通知詳情VO（包含用戶閱讀記錄和重發失敗記錄）
 */
public class FailedNotificationDetailVO {
    /** 通知ID */
    private Long notificationId;

    /** 通知標題 */
    private String title;

    /** 發送記錄ID */
    private Long sendRecordId;

    /** 發送時間 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sendTime;

    /** 發送狀態 */
    private String sendStatus;

    /** 應發送總人數 */
    private Integer totalCount;

    /** 發送成功人數 */
    private Integer successCount;

    /** 發送失敗人數 */
    private Integer failCount;

    /** 發送人姓名 */
    private String senderName;

    private List<ResendFailRecordVO> resendFailRecords;

    // Getters and Setters
    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getSendRecordId() {
        return sendRecordId;
    }

    public void setSendRecordId(Long sendRecordId) {
        this.sendRecordId = sendRecordId;
    }

    public LocalDateTime getSendTime() {
        return sendTime;
    }

    public void setSendTime(LocalDateTime sendTime) {
        this.sendTime = sendTime;
    }

    public String getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(String sendStatus) {
        this.sendStatus = sendStatus;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public List<ResendFailRecordVO> getResendFailRecords() {
        return resendFailRecords;
    }

    public void setResendFailRecords(List<ResendFailRecordVO> resendFailRecords) {
        this.resendFailRecords = resendFailRecords;
    }
}
