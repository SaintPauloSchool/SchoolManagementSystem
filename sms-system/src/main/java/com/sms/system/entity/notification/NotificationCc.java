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

    /** 抄送類型（1教職員工 2學校通訊錄） */
    @TableField("cc_type")
    private String ccType;

    /** 抄送數據(JSON格式)，格式：[{"cc_ids": [1,2], "type": 1, "cc_names": ["聖保祿學校-054"]}]，其中 type 1代表wecom的，type 2代表自定義的 */
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
