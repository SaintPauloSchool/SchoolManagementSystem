package com.sms.task;

import com.sms.handler.notification.NotificationPublishHandler;
import com.sms.scheduler.ScheduledTaskSupport;
import com.sms.handler.system.TaskLogHelper;
import com.sms.system.constant.ScheduledTaskKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 定時重新發送失敗通知的任務
 */
@Component
public class NotificationResendTask {

    private static final Logger log = LoggerFactory.getLogger(NotificationResendTask.class);

    @Autowired
    private NotificationPublishHandler notificationPublishHandler;

    @Autowired
    private TaskLogHelper taskLogHelper;

    @Autowired
    private ScheduledTaskSupport scheduledTaskSupport;

    private static final AtomicBoolean isExecuting = new AtomicBoolean(false);

    public void executeTask() {
        if (scheduledTaskSupport.shouldSkipForSchedule(ScheduledTaskKeys.NOTIFICATION_RESEND)) {
            return;
        }
        if (!isExecuting.compareAndSet(false, true)) {
            log.info("定時重新發送失敗通知任務已在執行中，跳過本次執行");
            return;
        }
        try {
            taskLogHelper.executeAndLog(
                "定時重新發送失敗通知",
                "notificationPublishHandler",
                "resendAllFailedNotifications",
                () -> notificationPublishHandler.resendAllFailedNotifications()
            );
        } catch (Exception e) {
            log.error("執行定時重新發送失敗通知任務異常", e);
        } finally {
            isExecuting.set(false);
        }
    }
}
