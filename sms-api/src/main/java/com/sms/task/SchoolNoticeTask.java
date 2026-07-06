package com.sms.task;

import com.sms.handler.notification.NotificationPublishHandler;
import com.sms.handler.TaskLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 學校通知定時發送任務
 * 每周一到周五下午 6 點執行，所有業務邏輯由 NotificationPublishHandler.sendDailySchoolNotice() 處理
 */
@Component
public class SchoolNoticeTask {

    private static final Logger log = LoggerFactory.getLogger(SchoolNoticeTask.class);

    @Autowired
    private NotificationPublishHandler notificationPublishHandler;

    @Autowired
    private TaskLogHelper taskLogHelper;

    private static final AtomicBoolean isExecuting = new AtomicBoolean(false);

    /**
     * 每周一到周五下午 6 點執行
     */
    //@Scheduled(cron = "0 0 18 ? * MON-FRI")
    public void executeTask() {
        if (!isExecuting.compareAndSet(false, true)) {
            log.info("每日學生手冊通知發送任務已在執行中，跳過本次執行");
            return;
        }
        try {
            taskLogHelper.executeAndLog(
                    "每日學生手冊通知發送",
                    "notificationPublishHandler",
                    "sendDailySchoolNotice",
                    () -> notificationPublishHandler.sendDailySchoolNotice()
            );
        } catch (Exception e) {
            log.error("執行每日學生手冊通知發送發生異常", e);
        } finally {
            isExecuting.set(false);
        }
    }
}
