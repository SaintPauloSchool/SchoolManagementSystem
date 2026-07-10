package com.sms.system.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sms.common.core.domain.BaseEntity;
import com.sms.common.json.LocalDateToDateTimeDeserializer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 通知實體類
 *
 */
@TableName("notification")
public class Notification extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 通知ID */
    @TableId(value = "notification_id", type = IdType.AUTO)
    private Long notificationId;

    /** 通知標題 */
    @TableField("title")
    private String title;

    /** 通知正文 */
    @TableField("content")
    private String content;

    /** 發送人ID */
    @TableField("sender_id")
    private Long senderId;

    /** 發送人姓名 */
    @TableField("sender_name")
    private String senderName;

    /** 跳轉鏈接 */
    @TableField("jump_url")
    private String jumpUrl;

    /** 附件/圖片URL列表(JSON格式) */
    @TableField("attachment_urls")
    private String attachmentUrls;

    /** 狀態（0草稿 1已發佈 2已撤回） */
    @TableField("status")
    private String status;

    /** 回復截止時間 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("reply_deadline")
    private LocalDateTime replyDeadline;

    /** 提示回覆時間（只到日期） */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @JsonDeserialize(using = LocalDateToDateTimeDeserializer.class)
    @TableField("reminder_time")
    private LocalDateTime reminderTime;
    
    /** 接收對象列表（非數據庫字段） */
    @TableField(exist = false)
    private List<NotificationReceiver> receivers;
    
    /** 抄送對象列表（非數據庫字段） */
    @TableField(exist = false)
    private List<NotificationCc> ccs;
    
    /** 問題列表（非數據庫字段） */
    @TableField(exist = false)
    private List<NotificationQuestion> questions;

    /** 發佈時間篩選（非數據庫字段） */
    @TableField(exist = false)
    private String publishDate;

    /** 用戶ID（非數據庫字段，用於抄送列表） */
    @TableField(exist = false)
    private Long userId;

    /** 用戶類型（非數據庫字段，用於抄送列表） */
    @TableField(exist = false)
    private String userType;

    /** 企業微信userid（非數據庫字段，用於抄送列表查詢部門ID） */
    @TableField(exist = false)
    private String openUserId;

    /** 通知ID列表（非數據庫字段，用於抄送列表查詢） */
    @TableField(exist = false)
    private Set<Long> notificationIds;

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

    public LocalDateTime getReplyDeadline() {
        return replyDeadline;
    }

    public void setReplyDeadline(LocalDateTime replyDeadline) {
        this.replyDeadline = replyDeadline;
    }

    public LocalDateTime getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(LocalDateTime reminderTime) {
        this.reminderTime = reminderTime;
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

    public String getOpenUserId() {
        return openUserId;
    }

    public void setOpenUserId(String openUserId) {
        this.openUserId = openUserId;
    }

    public Set<Long> getNotificationIds() {
        return notificationIds;
    }

    public void setNotificationIds(Set<Long> notificationIds) {
        this.notificationIds = notificationIds;
    }
}
