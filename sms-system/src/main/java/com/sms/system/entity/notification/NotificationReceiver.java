package com.sms.system.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知接收對象實體類
 *
 */
@TableName("notification_receiver")
public class NotificationReceiver implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 接收關係ID */
    @TableId(value = "receiver_id", type = IdType.AUTO)
    private Long receiverId;

    /** 通知ID */
    @TableField("notification_id")
    private Long notificationId;

    /** 接收類型（1 班級 2 學生/家長） */
    @TableField("receive_type")
    private String receiveType;

    /** 接收數據來源集合(存儲多源JSON) */
    @TableField("receive_data")
    private String receiveData;

    /** 創建時間 */
    @TableField("create_time")
    private LocalDateTime createTime;

    // Getters and Setters
    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
