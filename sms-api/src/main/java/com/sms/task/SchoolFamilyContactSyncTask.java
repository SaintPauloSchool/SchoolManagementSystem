package com.sms.task;

import com.sms.handler.wecom.WecomSyncHandler;
import com.sms.handler.ScheduledTaskSupport;
import com.sms.handler.TaskLogHelper;
import com.sms.system.constant.ScheduledTaskKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SchoolFamilyContactSyncTask {

    private static final Logger log = LoggerFactory.getLogger(SchoolFamilyContactSyncTask.class);

    @Autowired
    private WecomSyncHandler wecomSyncHandler;

    @Autowired
    private TaskLogHelper taskLogHelper;

    @Autowired
    private ScheduledTaskSupport scheduledTaskSupport;

    private static final AtomicBoolean isExecuting = new AtomicBoolean(false);

    @Scheduled(cron = "0 30 0 * * ?")
    public void executeTask() {
        if (scheduledTaskSupport.shouldSkipForSchedule(ScheduledTaskKeys.SCHOOL_FAMILY_CONTACT_SYNC)) {
            return;
        }
        if (!isExecuting.compareAndSet(false, true)) {
            log.info("家校通訊錄同步任務已在執行中，跳過本次執行");
            return;
        }
        try {
            taskLogHelper.executeAndLog(
                "家校通訊錄同步",
                "wecomSyncHandler",
                "syncSchoolFamilyContacts",
                () -> wecomSyncHandler.syncSchoolFamilyContacts()
            );
        } catch (Exception e) {
            log.error("同步家校通訊錄數據失敗", e);
        } finally {
            isExecuting.set(false);
        }
    }
}
