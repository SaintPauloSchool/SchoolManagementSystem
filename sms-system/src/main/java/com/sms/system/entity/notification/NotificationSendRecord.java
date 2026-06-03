package com.sms.system.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.sms.common.core.domain.BaseEntity;

import java.time.LocalDateTime;

/**
 * 發送通知記錄實體類（主表）
 *
 */
@TableName("notification_send_record")
public class NotificationSendRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 發送記錄ID */
    @TableId(value = "send_record_id", type = IdType.AUTO)
    private Long sendRecordId;

    /** 通知ID */
    @TableField("notification_id")
    private Long notificationId;

    /** 發送人ID */
    @TableField("sender_id")
    private Long senderId;

    /** 發送人姓名 */
    @TableField("sender_name")
    private String senderName;

    /** 發送時間 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("send_time")
    private LocalDateTime sendTime;

    /** 發送狀態（0待發送 1發送中 2發送成功 3發送失敗 4部分成功） */
    @TableField("send_status")
    private String sendStatus;

    /** 應發送總人數 */
    @TableField("total_count")
    private Integer totalCount;

    /** 發送成功人數 */
    @TableField("success_count")
    private Integer successCount;

    /** 發送失敗人數 */
    @TableField("fail_count")
    private Integer failCount;

    // Getters and Setters
    public Long getSendRecordId() {
        return sendRecordId;
    }

    public void setSendRecordId(Long sendRecordId) {
        this.sendRecordId = sendRecordId;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
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

    @Override
    public String toString() {
        return "NotificationSendRecord{" +
                "sendRecordId=" + sendRecordId +
                ", notificationId=" + notificationId +
                ", senderId=" + senderId +
                ", senderName='" + senderName + '\'' +
                ", sendTime=" + sendTime +
                ", sendStatus='" + sendStatus + '\'' +
                ", totalCount=" + totalCount +
                ", successCount=" + successCount +
                ", failCount=" + failCount +
                '}';
    }
}
