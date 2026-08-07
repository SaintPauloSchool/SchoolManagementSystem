package com.sms.system.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知提醒記錄實體類（用於收集提示家長回復的記錄）
 */
@TableName("notification_reminder_record")
public class NotificationReminderRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 提醒記錄ID */
    @TableId(value = "reminder_id", type = IdType.AUTO)
    private Long reminderId;

    /** 原通知ID */
    @TableField("notification_id")
    private Long notificationId;

    /** 原發送記錄ID */
    @TableField("send_record_id")
    private Long sendRecordId;

    /** 學籍 student_id（student_profiles.student_info.student_id） */
    @TableField("student_id")
    private String studentId;

    /** 未回復的家長用戶ID列表(JSON格式) */
    @TableField("parent_user_ids")
    private String parentUserIds;

    /** 提醒發送時間 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("remind_send_time")
    private LocalDateTime remindSendTime;

    /** 提醒發送狀態（0待發送 1發送成功 2發送失敗） */
    @TableField("remind_send_status")
    private String remindSendStatus;

    /** 創建時間 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;

    public Long getReminderId() {
        return reminderId;
    }

    public void setReminderId(Long reminderId) {
        this.reminderId = reminderId;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Long getSendRecordId() {
        return sendRecordId;
    }

    public void setSendRecordId(Long sendRecordId) {
        this.sendRecordId = sendRecordId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getParentUserIds() {
        return parentUserIds;
    }

    public void setParentUserIds(String parentUserIds) {
        this.parentUserIds = parentUserIds;
    }

    public LocalDateTime getRemindSendTime() {
        return remindSendTime;
    }

    public void setRemindSendTime(LocalDateTime remindSendTime) {
        this.remindSendTime = remindSendTime;
    }

    public String getRemindSendStatus() {
        return remindSendStatus;
    }

    public void setRemindSendStatus(String remindSendStatus) {
        this.remindSendStatus = remindSendStatus;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "NotificationReminderRecord{" +
                "reminderId=" + reminderId +
                ", notificationId=" + notificationId +
                ", sendRecordId=" + sendRecordId +
                ", studentId='" + studentId + '\'' +
                ", parentUserIds='" + parentUserIds + '\'' +
                ", remindSendTime=" + remindSendTime +
                ", remindSendStatus='" + remindSendStatus + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
