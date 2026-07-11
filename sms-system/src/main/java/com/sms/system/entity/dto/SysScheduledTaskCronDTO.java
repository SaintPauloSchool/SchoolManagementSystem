package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 定時任務 Cron 表達式更新 DTO
 */
public class SysScheduledTaskCronDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskKey;
    private String cronExpression;

    public String getTaskKey() {
        return taskKey;
    }

    public void setTaskKey(String taskKey) {
        this.taskKey = taskKey;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
}
