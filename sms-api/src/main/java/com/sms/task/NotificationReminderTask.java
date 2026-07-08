package com.sms.task;

import com.sms.handler.notification.NotificationPublishHandler;
import com.sms.handler.ScheduledTaskSupport;
import com.sms.handler.TaskLogHelper;
import com.sms.system.constant.ScheduledTaskKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 定時提示家長回復通知的任務
 * 每天 9 點 30 分執行，所有業務邏輯由 NotificationPublishHandler.remindAllPendingNotifications() 處理
 */
@Component
public class NotificationReminderTask {

    private static final Logger log = LoggerFactory.getLogger(NotificationReminderTask.class);

    @Autowired
    private NotificationPublishHandler notificationPublishHandler;

    @Autowired
    private TaskLogHelper taskLogHelper;

    @Autowired
    private ScheduledTaskSupport scheduledTaskSupport;

    private static final AtomicBoolean isExecuting = new AtomicBoolean(false);

    @Scheduled(cron = "0 30 9 * * ?")
    public void executeTask() {
        if (scheduledTaskSupport.shouldSkipForSchedule(ScheduledTaskKeys.NOTIFICATION_REMINDER)) {
            return;
        }
        if (!isExecuting.compareAndSet(false, true)) {
            log.info("定時提示家長回復通知任務已在執行中，跳過本次執行");
            return;
        }
        try {
            taskLogHelper.executeAndLog(
                "定時提示家長回復通知",
                "notificationPublishHandler",
                "remindAllPendingNotifications",
                () -> notificationPublishHandler.remindAllPendingNotifications()
            );
        } catch (Exception e) {
            log.error("執行定時提示家長回復通知任務異常", e);
        } finally {
            isExecuting.set(false);
        }
    }
}
