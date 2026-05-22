package com.sms.task;

import com.sms.handler.notification.NotificationPublishHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 定時重新發送失敗通知的任務
 * 每天 9 點到 18 點之間每小時執行，所有業務邏輯由 NotificationPublishHandler.resendAllFailedNotifications() 處理
 */
@Component
public class NotificationResendTask {

    private static final Logger log = LoggerFactory.getLogger(NotificationResendTask.class);

    @Autowired
    private NotificationPublishHandler notificationPublishHandler;

    private static final AtomicBoolean isExecuting = new AtomicBoolean(false);

    /**
     * 每天 9 點到 18 點之間每小時自動重發失敗的通知
     */
    //@Scheduled(cron = "0 0 9-18 * * ?")
    public void resendFailedNotificationsTask() {
        if (!isExecuting.compareAndSet(false, true)) {
            log.info("定時重新發送失敗通知任務已在執行中，跳過本次執行");
            return;
        }
        try {
            notificationPublishHandler.resendAllFailedNotifications();
        } catch (Exception e) {
            log.error("執行定時重新發送失敗通知任務異常", e);
        } finally {
            isExecuting.set(false);
        }
    }
}
