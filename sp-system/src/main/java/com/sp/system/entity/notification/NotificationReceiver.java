package com.sp.system.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知接收对象实体类
 *
 */
@TableName("notification_receiver")
public class NotificationReceiver implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 接收关系ID */
    @TableId(value = "receiver_id", type = IdType.AUTO)
    private Long receiverId;

    /** 通知ID */
    @TableField("notification_id")
    private Long notificationId;

    /** 接收类型（1 班级 2 学生/家长） */
    @TableField("receive_type")
    private String receiveType;

    /** 接收对象ID列表(JSON格式) */
    @TableField("receive_ids")
    private String receiveIds;

    /** 接收对象名称列表(JSON格式) */
    @TableField("receive_names")
    private String receiveNames;

    /** 创建时间 */
    @TableField("create_time")
    private Date createTime;

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

    public String getReceiveIds() {
        return receiveIds;
    }

    public void setReceiveIds(String receiveIds) {
        this.receiveIds = receiveIds;
    }

    public String getReceiveNames() {
        return receiveNames;
    }

    public void setReceiveNames(String receiveNames) {
        this.receiveNames = receiveNames;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}