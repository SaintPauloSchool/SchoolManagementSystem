package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 通知接收對象保存 DTO
 */
public class NotificationReceiverSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String receiveType;
    private String receiveData;
    /** 通知ID（級聯保存時由 Service 設置） */
    private Long notificationId;

    public String getReceiveType() {
        return receiveType;
    }

    public void setReceiveType(String receiveType) {
        this.receiveType = receiveType;
    }

    public String getReceiveData() {
        return receiveData;
    }

    public void setReceiveData(String receiveData) {
        this.receiveData = receiveData;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }
}
