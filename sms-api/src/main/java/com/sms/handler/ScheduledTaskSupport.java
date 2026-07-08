package com.sms.handler;

import com.sms.system.service.ISysScheduledTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 定時任務啟用狀態檢查輔助類
 */
@Component
public class ScheduledTaskSupport {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskSupport.class);

    private static final ThreadLocal<Boolean> MANUAL_TRIGGER = new ThreadLocal<>();

    @Autowired
    private ISysScheduledTaskService sysScheduledTaskService;

    /**
     * 標記當前線程為手動觸發（手動觸發不受 enabled 限制）
     */
    public void markManualTrigger() {
        MANUAL_TRIGGER.set(Boolean.TRUE);
    }

    /**
     * 清除手動觸發標記
     */
    public void clearManualTrigger() {
        MANUAL_TRIGGER.remove();
    }

    /**
     * 判斷定時調度是否應跳過執行（任務停用時跳過；手動觸發時不跳過）
     */
    public boolean shouldSkipForSchedule(String taskKey) {
        if (Boolean.TRUE.equals(MANUAL_TRIGGER.get())) {
            return false;
        }
        boolean enabled = sysScheduledTaskService.isEnabled(taskKey);
        if (!enabled) {
            log.info("定時任務已停用，跳過本次執行，taskKey={}", taskKey);
        }
        return !enabled;
    }
}
