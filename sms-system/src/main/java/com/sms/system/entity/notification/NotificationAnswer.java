package com.sms.system.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知回答實體類
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

    /** 問題ID */
    @TableField("question_id")
    private Long questionId;

    /** 用戶ID（parentUserId，僅學生/家長回答） */
    @TableField("user_id")
    private String userId;

    /** 學籍 student_id（student_profiles.student_info.student_id） */
    @TableField("student_id")
    private String studentId;

    /** 答案數據（JSON格式） */
    @TableField("answer_data")
    private String answerData;

    /** 創建時間 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;

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

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getAnswerData() {
        return answerData;
    }

    public void setAnswerData(String answerData) {
        this.answerData = answerData;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
