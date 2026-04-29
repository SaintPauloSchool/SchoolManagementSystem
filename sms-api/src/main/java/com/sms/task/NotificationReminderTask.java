package com.sms.task;

import com.sms.handler.notification.NotificationPublishHandler;
import com.sms.system.entity.notification.Notification;
import com.sms.system.service.notification.INotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 定时提示家长回复通知的任务
 */
@Component
public class NotificationReminderTask {

    private static final Logger log = LoggerFactory.getLogger(NotificationReminderTask.class);

    @Autowired
    private INotificationService notificationService;

    @Autowired
    private NotificationPublishHandler notificationPublishHandler;

    /**
     * 每天9点半定时查询当天需要提示回复的通知，并发送提醒
     */
    //@Scheduled(cron = "0 30 9 * * ?")
    public void remindParentsToReplyTask() {
        log.info("开始执行定时提示家长回复通知的任务...");

        try {
            // 查询所有状态为已发布的通知且提示回复时间为今天的通知
            Notification queryParam = new Notification();
            queryParam.setStatus("1"); // 1-已发布
            queryParam.setReminderTime(new Date());
            
            // 直接通过数据库查询符合条件的通知
            List<Notification> notificationList = notificationService.selectNotificationList(queryParam);
            
            int remindCount = 0;
            // 判断通知列表是否为空
            if (notificationList != null && !notificationList.isEmpty()) {
                // 遍历通知列表，发送提醒
                for (Notification notification : notificationList) {
                    log.info("发现需要提示回复的通知: 标题={}, ID={}", notification.getTitle(), notification.getNotificationId());
                    try {
                        // 发送提醒
                        notificationPublishHandler.remindParentsToReply(notification);
                        remindCount++;
                    } catch (Exception e) {
                        log.error("定时提示回复失败: notificationId={}", notification.getNotificationId(), e);
                    }
                }
            }
            
            log.info("定时提示家长回复通知的任务执行完成，共处理 {} 个通知", remindCount);
        } catch (Exception e) {
            log.error("执行定时提示家长回复通知任务异常", e);
        }
    }
}
