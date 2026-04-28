package com.sms.system.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知提醒记录实体类（用于收集提示家长回复的记录）
 */
@TableName("notification_reminder_record")
public class NotificationReminderRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 提醒记录ID */
    @TableId(value = "reminder_id", type = IdType.AUTO)
    private Long reminderId;

    /** 原通知ID */
    @TableField("notification_id")
    private Long notificationId;

    /** 原发送记录ID */
    @TableField("send_record_id")
    private Long sendRecordId;

    /** 学生用户ID */
    @TableField("student_user_id")
    private String studentUserId;

    /** 未回复的家长用户ID列表(JSON格式) */
    @TableField("parent_user_ids")
    private String parentUserIds;

    /** 提醒发送时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("remind_send_time")
    private Date remindSendTime;

    /** 提醒发送状态（0待发送 1发送成功 2发送失败） */
    @TableField("remind_send_status")
    private String remindSendStatus;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private Date createTime;

    // Getters and Setters
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

    public String getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(String studentUserId) {
        this.studentUserId = studentUserId;
    }

    public String getParentUserIds() {
        return parentUserIds;
    }

    public void setParentUserIds(String parentUserIds) {
        this.parentUserIds = parentUserIds;
    }

    public Date getRemindSendTime() {
        return remindSendTime;
    }

    public void setRemindSendTime(Date remindSendTime) {
        this.remindSendTime = remindSendTime;
    }

    public String getRemindSendStatus() {
        return remindSendStatus;
    }

    public void setRemindSendStatus(String remindSendStatus) {
        this.remindSendStatus = remindSendStatus;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "NotificationReminderRecord{" +
                "reminderId=" + reminderId +
                ", notificationId=" + notificationId +
                ", sendRecordId=" + sendRecordId +
                ", studentUserId='" + studentUserId + '\'' +
                ", parentUserIds='" + parentUserIds + '\'' +
                ", remindSendTime=" + remindSendTime +
                ", remindSendStatus='" + remindSendStatus + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
