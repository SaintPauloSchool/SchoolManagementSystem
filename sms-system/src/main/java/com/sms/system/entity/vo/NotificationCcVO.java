package com.sms.system.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知抄送對象 VO
 */
public class NotificationCcVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long ccId;
    private Long notificationId;
    private String ccType;
    private String ccData;

    /** 由 cc_data 中的成員 ID 解析出的名稱列表（非數據庫字段） */
    private List<String> ccNames;

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

    public List<String> getCcNames() {
        return ccNames;
    }

    public void setCcNames(List<String> ccNames) {
        this.ccNames = ccNames;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
