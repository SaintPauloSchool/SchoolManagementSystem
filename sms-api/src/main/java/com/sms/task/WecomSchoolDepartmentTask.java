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
 * 企業微信獲取部門列表定時任務
 * 每天凌晨 1 點執行，所有業務邏輯由 WecomSyncHandler.syncWecomDepartmentsAndMembers() 處理
 */
@Component
public class WecomSchoolDepartmentTask {

    private static final Logger log = LoggerFactory.getLogger(WecomSchoolDepartmentTask.class);

    @Autowired
    private WecomSyncHandler wecomSyncHandler;

    @Autowired
    private TaskLogHelper taskLogHelper;

    private static final AtomicBoolean isExecuting = new AtomicBoolean(false);

    /**
     * 每天凌晨 1 點執行（北京時間）
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void executeTask() {
        if (!isExecuting.compareAndSet(false, true)) {
            log.info("企業微信部門同步任務已在執行中，跳過本次執行");
            return;
        }
        try {
            taskLogHelper.executeAndLog(
                "企業微信部門與成員同步",
                "wecomSyncHandler",
                "syncWecomDepartmentsAndMembers",
                () -> wecomSyncHandler.syncWecomDepartmentsAndMembers()
            );
        } catch (Exception e) {
            log.error("執行企業微信部門同步任務失敗", e);
        } finally {
            isExecuting.set(false);
        }
    }
}
