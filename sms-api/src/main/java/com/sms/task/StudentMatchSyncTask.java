package com.sms.task;

import com.sms.handler.student.StudentMatchSyncHandler;
import com.sms.handler.system.TaskLogHelper;
import com.sms.scheduler.ScheduledTaskSupport;
import com.sms.system.constant.ScheduledTaskKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 學生數據自動匹配定時任務
 */
@Component
public class StudentMatchSyncTask {

    private static final Logger log = LoggerFactory.getLogger(StudentMatchSyncTask.class);

    @Autowired
    private StudentMatchSyncHandler studentMatchSyncHandler;

    @Autowired
    private TaskLogHelper taskLogHelper;

    @Autowired
    private ScheduledTaskSupport scheduledTaskSupport;

    private static final AtomicBoolean isExecuting = new AtomicBoolean(false);

    public void executeTask() {
        if (scheduledTaskSupport.shouldSkipForSchedule(ScheduledTaskKeys.STUDENT_MATCH_SYNC)) {
            return;
        }
        if (!isExecuting.compareAndSet(false, true)) {
            log.info("學生數據自動匹配任務已在執行中，跳過本次執行");
            return;
        }
        try {
            taskLogHelper.executeAndLog(
                    "學生數據自動匹配",
                    "studentMatchSyncHandler",
                    "syncStudentMatch",
                    () -> studentMatchSyncHandler.syncStudentMatch()
            );
        } catch (Exception e) {
            log.error("執行學生數據自動匹配任務失敗", e);
        } finally {
            isExecuting.set(false);
        }
    }
}
