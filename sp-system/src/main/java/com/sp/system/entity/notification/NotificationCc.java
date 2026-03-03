package com.sp.system.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知抄送对象实体类
 *
 */
@TableName("notification_cc")
public class NotificationCc implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 抄送关系ID */
    @TableId(value = "cc_id", type = IdType.AUTO)
    private Long ccId;

    /** 通知ID */
    @TableField("notification_id")
    private Long notificationId;

    /** 抄送类型（1教职员工 2学校通讯录） */
    @TableField("cc_type")
    private String ccType;

    /** 抄送对象ID列表(JSON格式) */
    @TableField("cc_ids")
    private String ccIds;

    /** 抄送对象名称列表(JSON格式) */
    @TableField("cc_names")
    private String ccNames;

    /** 创建时间 */
    @TableField("create_time")
    private Date createTime;

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

    public String getCcIds() {
        return ccIds;
    }

    public void setCcIds(String ccIds) {
        this.ccIds = ccIds;
    }

    public String getCcNames() {
        return ccNames;
    }

    public void setCcNames(String ccNames) {
        this.ccNames = ccNames;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}