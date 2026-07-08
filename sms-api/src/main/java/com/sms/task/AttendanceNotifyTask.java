package com.sms.task;

import com.sms.handler.attendance.AttendanceNotifyHandler;
import com.sms.handler.ScheduledTaskSupport;
import com.sms.system.constant.ScheduledTaskKeys;
import com.sms.system.entity.task.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 考勤拍卡通知定時任務
 * 每 1 分鐘掃描未通知的考勤記錄並向家長發送微信通知。
 */
@Component
public class AttendanceNotifyTask {

    private static final Logger log = LoggerFactory.getLogger(AttendanceNotifyTask.class);

    @Autowired
    private AttendanceNotifyHandler attendanceNotifyHandler;

    @Autowired
    private ScheduledTaskSupport scheduledTaskSupport;

    private static final AtomicBoolean isExecuting = new AtomicBoolean(false);

    @Scheduled(cron = "0 * * * * ?")
    public void executeTask() {
        if (scheduledTaskSupport.shouldSkipForSchedule(ScheduledTaskKeys.ATTENDANCE_NOTIFY)) {
            return;
        }
        if (!isExecuting.compareAndSet(false, true)) {
            log.debug("考勤拍卡通知任務已在執行中，跳過本次執行");
            return;
        }
        try {
            TaskResult result = attendanceNotifyHandler.processPendingNotifications();
            if (result != null && (result.getSuccessCount() > 0 || result.getFailCount() > 0)) {
                log.info("考勤拍卡通知執行完成: {}", result.getMessage());
            }
        } catch (Exception e) {
            log.error("執行考勤拍卡通知任務異常", e);
        } finally {
            isExecuting.set(false);
        }
    }
}
