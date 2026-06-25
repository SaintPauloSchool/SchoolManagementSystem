package com.sms.system.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sms.common.json.LocalDateToDateTimeDeserializer;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知保存 DTO
 */
public class NotificationSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long notificationId;
    /** 發送人ID（由 Controller 從登錄上下文設置） */
    private Long senderId;
    /** 發送人姓名（由 Controller 從登錄上下文設置） */
    private String senderName;
    private String title;
    private String content;
    private String jumpUrl;
    private String attachmentUrls;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = LocalDateToDateTimeDeserializer.class)
    private LocalDateTime replyDeadline;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @JsonDeserialize(using = LocalDateToDateTimeDeserializer.class)
    private LocalDateTime reminderTime;
    private List<NotificationReceiverSaveDTO> receivers;
    private List<NotificationCcSaveDTO> ccs;
    private List<NotificationQuestionSaveDTO> questions;

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

    public List<NotificationReceiverSaveDTO> getReceivers() {
        return receivers;
    }

    public void setReceivers(List<NotificationReceiverSaveDTO> receivers) {
        this.receivers = receivers;
    }

    public List<NotificationCcSaveDTO> getCcs() {
        return ccs;
    }

    public void setCcs(List<NotificationCcSaveDTO> ccs) {
        this.ccs = ccs;
    }

    public List<NotificationQuestionSaveDTO> getQuestions() {
        return questions;
    }

    public void setQuestions(List<NotificationQuestionSaveDTO> questions) {
        this.questions = questions;
    }
}
