package com.sms.task;

import com.sms.handler.notification.NotificationPublishHandler;
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

    private static final AtomicBoolean isExecuting = new AtomicBoolean(false);

    /**
     * 每周一到周五下午 6 點執行（北京時間）
     */
    //@Scheduled(cron = "0 0 18 ? * MON-FRI", zone = "Asia/Shanghai")
    public void sendSchoolNotice() {
        if (!isExecuting.compareAndSet(false, true)) {
            log.info("學校通知發送任務已在執行中，跳過本次執行");
            return;
        }
        try {
            notificationPublishHandler.sendDailySchoolNotice();
        } catch (Exception e) {
            log.error("定時發送學校通知失敗", e);
        } finally {
            isExecuting.set(false);
        }
    }
}
