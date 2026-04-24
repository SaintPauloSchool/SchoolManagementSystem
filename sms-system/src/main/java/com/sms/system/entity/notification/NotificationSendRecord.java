package com.sms.system.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.sms.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 发送通知记录实体类（主表）
 *
 */
@TableName("notification_send_record")
public class NotificationSendRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 发送记录ID */
    @TableId(value = "send_record_id", type = IdType.AUTO)
    private Long sendRecordId;

    /** 通知ID */
    @TableField("notification_id")
    private Long notificationId;

    /** 发送人ID */
    @TableField("sender_id")
    private Long senderId;

    /** 发送人姓名 */
    @TableField("sender_name")
    private String senderName;

    /** 发送时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("send_time")
    private Date sendTime;

    /** 发送状态（0待发送 1发送中 2发送成功 3发送失败 4部分成功） */
    @TableField("send_status")
    private String sendStatus;

    /** 应发送总人数 */
    @TableField("total_count")
    private Integer totalCount;

    /** 发送成功人数 */
    @TableField("success_count")
    private Integer successCount;

    /** 发送失败人数 */
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
