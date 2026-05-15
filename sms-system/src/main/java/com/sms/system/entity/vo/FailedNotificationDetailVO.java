package com.sms.system.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.List;

/**
 * 失败通知详情VO（包含用户阅读记录和重发失败记录）
 */
public class FailedNotificationDetailVO {
    /** 通知ID */
    private Long notificationId;

    /** 通知标题 */
    private String title;

    /** 发送记录ID */
    private Long sendRecordId;

    /** 发送时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date sendTime;

    /** 发送状态 */
    private String sendStatus;

    /** 应发送总人数 */
    private Integer totalCount;

    /** 发送成功人数 */
    private Integer successCount;

    /** 发送失败人数 */
    private Integer failCount;

    /** 发送人姓名 */
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

    public Date getSendTime() {
        return sendTime;
    }

    public void setSendTime(Date sendTime) {
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
