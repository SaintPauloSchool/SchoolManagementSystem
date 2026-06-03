package com.sms.system.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知用戶閱讀記錄實體類（從表）
 *
 */
@TableName("notification_user_read_record")
public class NotificationUserReadRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 閱讀記錄ID */
    @TableId(value = "read_id", type = IdType.AUTO)
    private Long readId;

    /** 發送記錄ID */
    @TableField("send_record_id")
    private Long sendRecordId;

    /** 用戶ID */
    @TableField("user_id")
    private String userId;

    /** 用戶類型（1學生 2家長 3教師） */
    @TableField("user_type")
    private String userType;

    /** 是否已讀（0未讀 1已讀） */
    @TableField("is_read")
    private String isRead;

    /** 閱讀時間 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("read_time")
    private LocalDateTime readTime;

    /** 回復狀態（0未回復 1已回復） */
    @TableField("reply_status")
    private String replyStatus;

    /** 回復時間 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("reply_time")
    private LocalDateTime replyTime;

    /** 發送狀態（0發送失敗 1發送成功） */
    @TableField("send_status")
    private String sendStatus;

    /** 關聯的學生ID（當接收者是家長時記錄，若發送給學生本身則與userId相同） */
    @TableField("student_user_id")
    private String studentUserId;

    /** 創建時間 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;

    // Getters and Setters
    public Long getReadId() {
        return readId;
    }

    public void setReadId(Long readId) {
        this.readId = readId;
    }

    public Long getSendRecordId() {
        return sendRecordId;
    }

    public void setSendRecordId(Long sendRecordId) {
        this.sendRecordId = sendRecordId;
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

    public String getIsRead() {
        return isRead;
    }

    public void setIsRead(String isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getReadTime() {
        return readTime;
    }

    public void setReadTime(LocalDateTime readTime) {
        this.readTime = readTime;
    }

    public String getReplyStatus() {
        return replyStatus;
    }

    public void setReplyStatus(String replyStatus) {
        this.replyStatus = replyStatus;
    }

    public LocalDateTime getReplyTime() {
        return replyTime;
    }

    public void setReplyTime(LocalDateTime replyTime) {
        this.replyTime = replyTime;
    }

    public String getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(String sendStatus) {
        this.sendStatus = sendStatus;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(String studentUserId) {
        this.studentUserId = studentUserId;
    }

    @Override
    public String toString() {
        return "NotificationUserReadRecord{" +
                "readId=" + readId +
                ", sendRecordId=" + sendRecordId +
                ", userId=" + userId +
                ", userType='" + userType + '\'' +
                ", isRead='" + isRead + '\'' +
                ", readTime=" + readTime +
                ", replyStatus='" + replyStatus + '\'' +
                ", replyTime=" + replyTime +
                ", sendStatus='" + sendStatus + '\'' +
                ", studentUserId='" + studentUserId + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
