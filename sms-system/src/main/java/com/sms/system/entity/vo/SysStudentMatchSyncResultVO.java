package com.sms.system.entity.vo;

import java.io.Serializable;

/**
 * 批量同步學生姓名至企微結果
 */
public class SysStudentMatchSyncResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int successCount;
    private int failCount;
    private String message;

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
