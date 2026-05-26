package com.sms.task;

import com.sms.handler.wecom.WecomSyncHandler;
import com.sms.handler.TaskLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 家校通訊錄部門數據同步定時任務
 * 每天凌晨 0 點執行，所有業務邏輯由 WecomSyncHandler.syncSchoolDepartments() 處理
 */
@Component
public class DepartmentSyncTask {

    private static final Logger log = LoggerFactory.getLogger(DepartmentSyncTask.class);

    @Autowired
    private WecomSyncHandler wecomSyncHandler;

    @Autowired
    private TaskLogHelper taskLogHelper;

    private static final AtomicBoolean isExecuting = new AtomicBoolean(false);

    /**
     * 每天凌晨 0 點執行
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void executeTask() {
        if (!isExecuting.compareAndSet(false, true)) {
            log.info("部門數據同步任務已在執行中，跳過本次執行");
            return;
        }
        try {
            taskLogHelper.executeAndLog(
                "家校通訊錄部門同步",
                "wecomSyncHandler",
                "syncSchoolDepartments",
                () -> wecomSyncHandler.syncSchoolDepartments()
            );
        } catch (Exception e) {
            log.error("同步部門數據失敗", e);
        } finally {
            isExecuting.set(false);
        }
    }
}
