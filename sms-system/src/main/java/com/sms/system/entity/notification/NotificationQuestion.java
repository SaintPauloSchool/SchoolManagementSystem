package com.sms.system.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知問題實體類
 *
 */
@TableName("notification_question")
public class NotificationQuestion implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 問題ID */
    @TableId(value = "question_id", type = IdType.AUTO)
    private Long questionId;

    /** 通知ID */
    @TableField("notification_id")
    private Long notificationId;

    /** 父問題ID（用於分支問題） */
    @TableField("parent_question_id")
    private Long parentQuestionId;

    /** 問題標題 */
    @TableField("question_title")
    private String questionTitle;

    /** 問題類型（1 單選 2 多選 3 填空 4 附件上傳 5 邏輯表單） */
    @TableField("question_type")
    private String questionType;
    
    /** 選項列表 (JSON 格式，適用於單選、多選)
     * 單選/多選格式：["選項 1","選項 2",...]
     * 邏輯表單題型的選項和數據存儲在 content 字段中
     */
    @TableField("options")
    private String options;

    /** 是否必答（0否 1是） */
    @TableField("is_required")
    private String isRequired;

    /** 排序 */
    @TableField("sort_order")
    private Integer sortOrder;
    
    /** 跳轉邏輯規則 (JSON 格式) */
    @TableField("logic_rules")
    private String logicRules;
    
    /** 填空題的填空列表 (JSON 格式) */
    @TableField("fill_blanks")
    private String fillBlanks;
    
    /** 填空題的正確答案 (JSON 格式) */
    @TableField("correct_answers")
    private String correctAnswers;
    
    /** 題目內容（富文本/HTML） */
    @TableField("content")
    private String content;

    /** 創建時間 */
    @TableField("create_time")
    private LocalDateTime createTime;

    // Getters and Setters
    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Long getParentQuestionId() {
        return parentQuestionId;
    }

    public void setParentQuestionId(Long parentQuestionId) {
        this.parentQuestionId = parentQuestionId;
    }

    public String getQuestionTitle() {
        return questionTitle;
    }

    public void setQuestionTitle(String questionTitle) {
        this.questionTitle = questionTitle;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(String isRequired) {
        this.isRequired = isRequired;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public String getLogicRules() {
        return logicRules;
    }
    
    public void setLogicRules(String logicRules) {
        this.logicRules = logicRules;
    }
    
    public String getFillBlanks() {
        return fillBlanks;
    }
    
    public void setFillBlanks(String fillBlanks) {
        this.fillBlanks = fillBlanks;
    }
    
    public String getCorrectAnswers() {
        return correctAnswers;
    }
    
    public void setCorrectAnswers(String correctAnswers) {
        this.correctAnswers = correctAnswers;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
}
