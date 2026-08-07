package com.sms.system.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知抄送對象實體類
 *
 */
@TableName("notification_cc")
public class NotificationCc implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 抄送關係ID */
    @TableId(value = "cc_id", type = IdType.AUTO)
    private Long ccId;

    /** 通知ID */
    @TableField("notification_id")
    private Long notificationId;

    /** 抄送來源類型 */
    @TableField("cc_type")
    private String ccType;

    /** 抄送成員 ID 列表（JSON 數組），如 [1,2,3] */
    @TableField("cc_data")
    private String ccData;

    /** 創建時間 */
    @TableField("create_time")
    private LocalDateTime createTime;

    // Getters and Setters
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
