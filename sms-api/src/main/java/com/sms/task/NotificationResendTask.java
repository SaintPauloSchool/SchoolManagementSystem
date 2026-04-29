package com.sms.task;

import com.sms.handler.notification.NotificationPublishHandler;
import com.sms.system.entity.notification.NotificationSendRecord;
import com.sms.system.service.notification.INotificationSendRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时重新发送失败通知的任务
 * 每天9点到18点之间每小时检查当天发送失败（全部失败或部分失败）的通知，并自动重新发送
 */
@Component
public class NotificationResendTask {

    private static final Logger log = LoggerFactory.getLogger(NotificationResendTask.class);

    @Autowired
    private INotificationSendRecordService notificationSendRecordService;

    @Autowired
    private NotificationPublishHandler notificationPublishHandler;

    /**
     * 每天9点到18点之间每小时自动发送失败的通知
     */
    //@Scheduled(cron = "0 0 9-18 * * ?")
    public void resendFailedNotificationsTask() {
        log.info("开始执行定时重新发送失败通知的任务...");

        try {
            // 查询所有发送失败（全部失败 status=3 或部分失败 status=4）的发送记录
            List<NotificationSendRecord> failedSendRecords =
                notificationSendRecordService.selectAllFailedRecords();

            if (failedSendRecords == null || failedSendRecords.isEmpty()) {
                log.info("没有发送失败的通知记录，任务结束");
                return;
            }

            log.info("共有 {} 条发送失败的通知记录，开始逐一重发", failedSendRecords.size());

            int successNotifications = 0;
            int failNotifications = 0;

            // 遍历发送失败的通知记录
            for (NotificationSendRecord sendRecord : failedSendRecords) {
                Long notificationId = sendRecord.getNotificationId();
                try {
                    log.info("重发失败通知: notificationId={}", notificationId);
                    // 重发失败的通知
                    notificationPublishHandler.resendFailedNotifications(notificationId, true);
                    successNotifications++;
                } catch (Exception e) {
                    log.error("重发失败通知异常: notificationId={}", notificationId, e);
                    failNotifications++;
                }
            }

            log.info("定时重新发送失败通知任务执行完成，成功处理 {} 个通知，失败 {} 个通知",
                successNotifications, failNotifications);

        } catch (Exception e) {
            log.error("执行定时重新发送失败通知任务异常", e);
        }
    }
}
