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
 * 家長學生關係同步定時任務
 * 每天凌晨 0 點 30 分執行，所有業務邏輯由 WecomSyncHandler.syncParentStudentRelations() 處理
 */
@Component
public class ParentStudentRelationSyncTask {

    private static final Logger log = LoggerFactory.getLogger(ParentStudentRelationSyncTask.class);

    @Autowired
    private WecomSyncHandler wecomSyncHandler;

    @Autowired
    private TaskLogHelper taskLogHelper;

    private static final AtomicBoolean isExecuting = new AtomicBoolean(false);

    /**
     * 每天凌晨 0 點 30 分執行
     */
    @Scheduled(cron = "0 30 0 * * ?")
    public void executeTask() {
        if (!isExecuting.compareAndSet(false, true)) {
            log.info("家長學生關係同步任務已在執行中，跳過本次執行");
            return;
        }
        try {
            taskLogHelper.executeAndLog(
                "家長學生關係同步",
                "wecomSyncHandler",
                "syncParentStudentRelations",
                () -> wecomSyncHandler.syncParentStudentRelations()
            );
        } catch (Exception e) {
            log.error("同步家長學生關係數據失敗", e);
        } finally {
            isExecuting.set(false);
        }
    }
}
