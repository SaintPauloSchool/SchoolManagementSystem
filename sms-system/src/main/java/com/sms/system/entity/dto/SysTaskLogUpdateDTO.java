package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 定時任務日誌更新請求
 */
public class SysTaskLogUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long logId;
    private String isProcessed;
    private String status;
    private String failReason;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getIsProcessed() {
        return isProcessed;
    }

    public void setIsProcessed(String isProcessed) {
        this.isProcessed = isProcessed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }
}
