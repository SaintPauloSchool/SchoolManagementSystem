package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 定時任務日誌查詢條件
 */
public class SysTaskLogQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String taskName;
    private String status;
    private String isProcessed;
    private String beginTime;
    private String endTime;

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIsProcessed() {
        return isProcessed;
    }

    public void setIsProcessed(String isProcessed) {
        this.isProcessed = isProcessed;
    }

    public String getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(String beginTime) {
        this.beginTime = beginTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
