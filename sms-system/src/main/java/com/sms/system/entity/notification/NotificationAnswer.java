package com.sms.system.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知回答实体类
 */
@TableName("notification_answer")
public class NotificationAnswer implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 答案ID */
    @TableId(value = "answer_id", type = IdType.AUTO)
    private Long answerId;

    /** 通知ID */
    @TableField("notification_id")
    private Long notificationId;

    /** 问题ID */
    @TableField("question_id")
    private Long questionId;

    /** 用户ID（parentUserId） */
    @TableField("user_id")
    private String userId;

    /** 用户类型（1 学生 2 家长 3 教师） */
    @TableField("user_type")
    private String userType;

    /** 答案数据（JSON格式） */
    @TableField("answer_data")
    private String answerData;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private Date createTime;

    public Long getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Long answerId) {
        this.answerId = answerId;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getAnswerData() {
        return answerData;
    }

    public void setAnswerData(String answerData) {
        this.answerData = answerData;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
