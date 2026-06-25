package com.sms.system.entity.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 通知列表查詢條件
 */
public class NotificationQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String title;
    private String status;
    private String publishDate;
    private Long userId;
    private String userType;
    private String openUserId;
    private Long senderId;
    private Set<Long> notificationIds;
    private LocalDateTime reminderTime;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Set<Long> getNotificationIds() {
        return notificationIds;
    }

    public void setNotificationIds(Set<Long> notificationIds) {
        this.notificationIds = notificationIds;
    }

    public LocalDateTime getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(LocalDateTime reminderTime) {
        this.reminderTime = reminderTime;
    }
}
