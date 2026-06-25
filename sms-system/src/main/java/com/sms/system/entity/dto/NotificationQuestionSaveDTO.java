package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 通知問題保存 DTO
 */
public class NotificationQuestionSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long questionId;
    private Long notificationId;
    private Long parentQuestionId;
    private String questionTitle;
    private String questionType;
    private String options;
    private String isRequired;
    private Integer sortOrder;
    private String logicRules;
    private String fillBlanks;
    private String correctAnswers;
    private String content;

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
