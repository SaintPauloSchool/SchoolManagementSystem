package com.sms.system.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知抄送對象 VO
 */
public class NotificationCcVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long ccId;
    private Long notificationId;
    private String ccType;
    private String ccData;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    public Long getCcId() {
        return ccId;
    }

    public void setCcId(Long ccId) {
        this.ccId = ccId;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
