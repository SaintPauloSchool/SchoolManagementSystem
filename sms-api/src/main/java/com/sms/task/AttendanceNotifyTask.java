package com.sms.task;

import com.sms.handler.attendance.AttendanceNotifyHandler;
import com.sms.handler.TaskLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 考勤拍卡通知定時任務
 * 每 1 分鐘掃描未通知的考勤記錄並向家長發送微信通知
 */
@Component
public class AttendanceNotifyTask {

    private static final Logger log = LoggerFactory.getLogger(AttendanceNotifyTask.class);

    @Autowired
    private AttendanceNotifyHandler attendanceNotifyHandler;

    @Autowired
    private TaskLogHelper taskLogHelper;

    private static final AtomicBoolean isExecuting = new AtomicBoolean(false);

    /**
     * 每分鐘執行一次
     */
    @Scheduled(cron = "0 * * * * ?")
    public void executeTask() {
        if (!isExecuting.compareAndSet(false, true)) {
            log.info("考勤拍卡通知任務已在執行中，跳過本次執行");
            return;
        }
        try {
            taskLogHelper.executeAndLog(
                    "考勤拍卡通知發送",
                    "attendanceNotifyHandler",
                    "processPendingNotifications",
                    attendanceNotifyHandler::processPendingNotifications
            );
        } catch (Exception e) {
            log.error("執行考勤拍卡通知任務異常", e);
        } finally {
            isExecuting.set(false);
        }
    }
}
