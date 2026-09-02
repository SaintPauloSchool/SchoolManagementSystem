package com.sms.system.service;

/**
 * 定時任務執行鎖（基於 sys_scheduled_task.lock_until）
 */
public interface IScheduledTaskExecutionLockService {

    /**
     * 嘗試獲取任務執行鎖
     *
     * @return true 表示獲取成功，可執行任務
     */
    boolean tryAcquire(String taskKey);

    /**
     * 釋放任務執行鎖
     */
    void release(String taskKey);
}
