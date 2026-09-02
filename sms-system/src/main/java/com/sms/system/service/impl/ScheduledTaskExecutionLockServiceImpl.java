package com.sms.system.service.impl;

import com.sms.common.utils.StringUtils;
import com.sms.system.mapper.SysScheduledTaskMapper;
import com.sms.system.service.IScheduledTaskExecutionLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;

/**
 * 使用資料庫行鎖避免同一任務被重複調度執行（多實例 / Tomcat 熱部署場景）。
 */
@Service
public class ScheduledTaskExecutionLockServiceImpl implements IScheduledTaskExecutionLockService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskExecutionLockServiceImpl.class);

    /** 單次任務最長租約（秒），超時後允許其他調度搶鎖 */
    private static final int DEFAULT_LEASE_SECONDS = 7200;

    private final String lockOwner = ManagementFactory.getRuntimeMXBean().getName();

    @Autowired
    private SysScheduledTaskMapper sysScheduledTaskMapper;

    @Override
    public boolean tryAcquire(String taskKey) {
        if (StringUtils.isEmpty(taskKey)) {
            return false;
        }
        int rows = sysScheduledTaskMapper.tryAcquireLock(taskKey, lockOwner, DEFAULT_LEASE_SECONDS);
        if (rows <= 0) {
            log.warn("定時任務 {} 跳過執行：已有實例持有執行鎖", taskKey);
            return false;
        }
        return true;
    }

    @Override
    public void release(String taskKey) {
        if (StringUtils.isEmpty(taskKey)) {
            return;
        }
        try {
            sysScheduledTaskMapper.releaseLock(taskKey, lockOwner);
        } catch (Exception e) {
            log.error("釋放定時任務鎖失敗，taskKey={}", taskKey, e);
        }
    }
}
