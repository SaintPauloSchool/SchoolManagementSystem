package com.sp.system.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知回答实体类
 *
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

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 用户类型（1学生 2家长 3教师） */
    @TableField("user_type")
    private String userType;

    /** 答案内容 */
    @TableField("answer_content")
    private String answerContent;

    /** 附件URL列表(JSON格式) */
    @TableField("attachment_urls")
    private String attachmentUrls;

    /** 创建时间 */
    @TableField("create_time")
    private Date createTime;

    // Getters and Setters
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getAnswerContent() {
        return answerContent;
    }

    public void setAnswerContent(String answerContent) {
        this.answerContent = answerContent;
    }

    public String getAttachmentUrls() {
        return attachmentUrls;
    }

    public void setAttachmentUrls(String attachmentUrls) {
        this.attachmentUrls = attachmentUrls;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}