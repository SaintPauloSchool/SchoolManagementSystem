package com.sms.system.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.sms.common.core.domain.BaseEntity;

import java.util.Date;
import java.util.List;

/**
 * 通知实体类
 *
 */
@TableName("notification")
public class Notification extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 通知ID */
    @TableId(value = "notification_id", type = IdType.AUTO)
    private Long notificationId;

    /** 通知标题 */
    @TableField("title")
    private String title;

    /** 通知正文 */
    @TableField("content")
    private String content;

    /** 发送人ID */
    @TableField("sender_id")
    private Long senderId;

    /** 发送人姓名 */
    @TableField("sender_name")
    private String senderName;

    /** 跳转链接 */
    @TableField("jump_url")
    private String jumpUrl;

    /** 附件/图片URL列表(JSON格式) */
    @TableField("attachment_urls")
    private String attachmentUrls;

    /** 状态（0草稿 1已发布 2已撤回） */
    @TableField("status")
    private String status;

    /** 回复截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("reply_deadline")
    private Date replyDeadline;
    
    /** 接收对象列表（非数据库字段） */
    @TableField(exist = false)
    private List<NotificationReceiver> receivers;
    
    /** 抄送对象列表（非数据库字段） */
    @TableField(exist = false)
    private List<NotificationCc> ccs;
    
    /** 问题列表（非数据库字段） */
    @TableField(exist = false)
    private List<NotificationQuestion> questions;

    /** 发布时间筛选（非数据库字段） */
    @TableField(exist = false)
    private String publishDate;

    /** 用户ID（非数据库字段，用于抄送列表） */
    @TableField(exist = false)
    private Long userId;

    /** 用户类型（非数据库字段，用于抄送列表） */
    @TableField(exist = false)
    private String userType;

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public String getJumpUrl() {
        return jumpUrl;
    }

    public void setJumpUrl(String jumpUrl) {
        this.jumpUrl = jumpUrl;
    }

    public String getAttachmentUrls() {
        return attachmentUrls;
    }

    public void setAttachmentUrls(String attachmentUrls) {
        this.attachmentUrls = attachmentUrls;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getReplyDeadline() {
        return replyDeadline;
    }

    public void setReplyDeadline(Date replyDeadline) {
        this.replyDeadline = replyDeadline;
    }
    
    public List<NotificationReceiver> getReceivers() {
        return receivers;
    }
    
    public void setReceivers(List<NotificationReceiver> receivers) {
        this.receivers = receivers;
    }
    
    public List<NotificationCc> getCcs() {
        return ccs;
    }
    
    public void setCcs(List<NotificationCc> ccs) {
        this.ccs = ccs;
    }
    
    public List<NotificationQuestion> getQuestions() {
        return questions;
    }
    
    public void setQuestions(List<NotificationQuestion> questions) {
        this.questions = questions;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
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
}
