package com.sms.system.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知问题实体类
 *
 */
@TableName("notification_question")
public class NotificationQuestion implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 问题ID */
    @TableId(value = "question_id", type = IdType.AUTO)
    private Long questionId;

    /** 通知ID */
    @TableField("notification_id")
    private Long notificationId;

    /** 父问题ID（用于分支问题） */
    @TableField("parent_question_id")
    private Long parentQuestionId;

    /** 问题标题 */
    @TableField("question_title")
    private String questionTitle;

    /** 问题类型（1 单选 2 多选 3 填空 4 附件上传 5 逻辑表单） */
    @TableField("question_type")
    private String questionType;
    
    /** 选项列表 (JSON 格式，适用于单选、多选)
     * 单选/多选格式：["选项 1","选项 2",...]
     * 逻辑表单题型的选项和数据存储在 content 字段中
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

    /** 创建时间 */
    @TableField("create_time")
    private Date createTime;

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

    public Date getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Date createTime) {
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
