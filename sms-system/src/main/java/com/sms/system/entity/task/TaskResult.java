package com.sms.system.entity.task;

public class TaskResult {
    private int successCount;
    private int failCount;
    private String message;

    public TaskResult(int successCount, int failCount, String message) {
        this.successCount = successCount;
        this.failCount = failCount;
        this.message = message;
    }

    public static TaskResult success(int successCount, int failCount, String message) {
        return new TaskResult(successCount, failCount, message);
    }

    public static TaskResult fail(int successCount, int failCount, String message) {
        return new TaskResult(successCount, failCount, message);
    }

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
