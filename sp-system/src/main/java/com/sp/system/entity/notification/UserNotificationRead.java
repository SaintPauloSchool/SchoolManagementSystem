package com.sp.system.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户通知阅读状态实体类
 *
 */
@TableName("user_notification_read")
public class UserNotificationRead implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 阅读记录ID */
    @TableId(value = "read_id", type = IdType.AUTO)
    private Long readId;

    /** 通知ID */
    @TableField("notification_id")
    private Long notificationId;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 用户类型（1学生 2家长 3教师） */
    @TableField("user_type")
    private String userType;

    /** 是否已读（0未读 1已读） */
    @TableField("is_read")
    private String isRead;

    /** 阅读时间 */
    @TableField("read_time")
    private Date readTime;

    /** 回复状态（0未回复 1已回复） */
    @TableField("reply_status")
    private String replyStatus;

    /** 回复时间 */
    @TableField("reply_time")
    private Date replyTime;

    /** 创建时间 */
    @TableField("create_time")
    private Date createTime;

    // Getters and Setters
    public Long getReadId() {
        return readId;
    }

    public void setReadId(Long readId) {
        this.readId = readId;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
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

    public Date getReadTime() {
        return readTime;
    }

    public void setReadTime(Date readTime) {
        this.readTime = readTime;
    }

    public String getReplyStatus() {
        return replyStatus;
    }

    public void setReplyStatus(String replyStatus) {
        this.replyStatus = replyStatus;
    }

    public Date getReplyTime() {
        return replyTime;
    }

    public void setReplyTime(Date replyTime) {
        this.replyTime = replyTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}