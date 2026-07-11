package com.sms.task;

import com.sms.handler.wecom.WecomSyncHandler;
import com.sms.scheduler.ScheduledTaskSupport;
import com.sms.handler.system.TaskLogHelper;
import com.sms.system.constant.ScheduledTaskKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 家校通訊錄部門數據同步定時任務
 */
@Component
public class DepartmentSyncTask {

    private static final Logger log = LoggerFactory.getLogger(DepartmentSyncTask.class);

    @Autowired
    private WecomSyncHandler wecomSyncHandler;

    @Autowired
    private TaskLogHelper taskLogHelper;

    @Autowired
    private ScheduledTaskSupport scheduledTaskSupport;

    private static final AtomicBoolean isExecuting = new AtomicBoolean(false);

    public void executeTask() {
        if (scheduledTaskSupport.shouldSkipForSchedule(ScheduledTaskKeys.DEPARTMENT_SYNC)) {
            return;
        }
        if (!isExecuting.compareAndSet(false, true)) {
            log.info("部門數據同步任務已在執行中，跳過本次執行");
            return;
        }
        try {
            taskLogHelper.executeAndLog(
                "家校通訊錄部門數據同步",
                "wecomSyncHandler",
                "syncSchoolDepartments",
                () -> wecomSyncHandler.syncSchoolDepartments()
            );
        } catch (Exception e) {
            log.error("執行部門數據同步任務失敗", e);
        } finally {
            isExecuting.set(false);
        }
    }
}
