package com.sms.task;

import com.sms.handler.TaskLogHelper;
import com.sms.handler.system.TaskMonitorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 檢查失敗任務並通知管理員
 * 每天早上 9:00 執行
 */
@Component("failedTaskNotifierTask")
public class FailedTaskNotifierTask {

    private static final Logger log = LoggerFactory.getLogger(FailedTaskNotifierTask.class);

    @Autowired
    private TaskMonitorHandler taskMonitorHandler;

    @Autowired
    private TaskLogHelper taskLogHelper;

    private static final AtomicBoolean isExecuting = new AtomicBoolean(false);

    /**
     * 每天早上 9:00 執行
     */
    //@Scheduled(cron = "0 0 9 * * ?")
    public void executeTask() {
        if (!isExecuting.compareAndSet(false, true)) {
            log.info("檢查失敗任務通知已在執行中，跳過本次執行");
            return;
        }
        try {
            taskLogHelper.executeAndLog(
                    "檢查失敗任務通知",
                    "taskMonitorHandler",
                    "checkAndNotifyFailedTasks",
                    () -> taskMonitorHandler.checkAndNotifyFailedTasks()
            );
        } catch (Exception e) {
            log.error("執行檢查失敗任務通知發生異常", e);
        } finally {
            isExecuting.set(false);
        }
    }
}
