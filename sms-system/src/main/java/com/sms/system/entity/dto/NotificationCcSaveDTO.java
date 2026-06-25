package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 通知抄送對象保存 DTO
 */
public class NotificationCcSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String ccType;
    private String ccData;
    /** 通知ID（級聯保存時由 Service 設置） */
    private Long notificationId;

    public String getCcType() {
        return ccType;
    }

    public void setCcType(String ccType) {
        this.ccType = ccType;
    }

    public String getCcData() {
        return ccData;
    }

    public void setCcData(String ccData) {
        this.ccData = ccData;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }
}
