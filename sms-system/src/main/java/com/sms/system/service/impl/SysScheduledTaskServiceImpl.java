package com.sms.system.service.impl;

import com.sms.system.entity.SysScheduledTask;
import com.sms.system.entity.SysTaskLog;
import com.sms.system.entity.dto.SysScheduledTaskCronDTO;
import com.sms.system.entity.dto.SysScheduledTaskStatusDTO;
import com.sms.system.entity.vo.SysScheduledTaskVO;
import com.sms.system.mapper.SysScheduledTaskMapper;
import com.sms.system.mapper.SysTaskLogMapper;
import com.sms.system.service.ISysScheduledTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定時任務配置 Service 實現
 */
@Service
public class SysScheduledTaskServiceImpl implements ISysScheduledTaskService {

    private static final Logger log = LoggerFactory.getLogger(SysScheduledTaskServiceImpl.class);

    @Autowired
    private SysScheduledTaskMapper sysScheduledTaskMapper;

    @Autowired
    private SysTaskLogMapper sysTaskLogMapper;

    @Override
    public List<SysScheduledTaskVO> selectTaskList() {
        List<SysScheduledTaskVO> list = sysScheduledTaskMapper.selectTaskList();
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        fillLatestExecutionInfo(list);
        return list;
    }

    private void fillLatestExecutionInfo(List<SysScheduledTaskVO> tasks) {
        List<SysTaskLog> latestLogs = sysTaskLogMapper.selectLatestLogPerTask();
        if (latestLogs == null || latestLogs.isEmpty()) {
            return;
        }
        Map<String, SysTaskLog> latestLogMap = new HashMap<>();
        for (SysTaskLog taskLog : latestLogs) {
            if (taskLog != null && StringUtils.hasText(taskLog.getTaskName())) {
                latestLogMap.put(taskLog.getTaskName(), taskLog);
            }
        }
        for (SysScheduledTaskVO task : tasks) {
            SysTaskLog latestLog = latestLogMap.get(task.getTaskName());
            if (latestLog != null) {
                task.setLastExecutionTime(latestLog.getExecutionTime());
                task.setLastStatus(latestLog.getStatus());
            }
        }
    }

    @Override
    public boolean isEnabled(String taskKey) {
        if (!StringUtils.hasText(taskKey)) {
            return false;
        }
        SysScheduledTask task = sysScheduledTaskMapper.selectByTaskKey(taskKey);
        if (task == null) {
            log.warn("定時任務配置不存在，taskKey={}", taskKey);
            return false;
        }
        return "1".equals(task.getEnabled());
    }

    @Override
    public int updateEnabled(SysScheduledTaskStatusDTO statusDTO) {
        if (statusDTO == null || !StringUtils.hasText(statusDTO.getTaskKey())) {
            throw new IllegalArgumentException("taskKey 不能為空");
        }
        String enabled = statusDTO.getEnabled();
        if (!"0".equals(enabled) && !"1".equals(enabled)) {
            throw new IllegalArgumentException("enabled 只能為 0 或 1");
        }

        SysScheduledTask existing = sysScheduledTaskMapper.selectByTaskKey(statusDTO.getTaskKey());
        if (existing == null) {
            throw new IllegalArgumentException("找不到定時任務: " + statusDTO.getTaskKey());
        }

        int rows = sysScheduledTaskMapper.updateEnabled(statusDTO.getTaskKey(), enabled);
        if (rows > 0) {
            log.info("定時任務 {} 已{}", statusDTO.getTaskKey(), "1".equals(enabled) ? "啟用" : "停用");
        }
        return rows;
    }

    @Override
    public int updateCronExpression(SysScheduledTaskCronDTO cronDTO) {
        if (cronDTO == null || !StringUtils.hasText(cronDTO.getTaskKey())) {
            throw new IllegalArgumentException("taskKey 不能為空");
        }
        String cronExpression = cronDTO.getCronExpression();
        if (!StringUtils.hasText(cronExpression)) {
            throw new IllegalArgumentException("Cron 表達式不能為空");
        }
        cronExpression = cronExpression.trim();
        if (!org.springframework.scheduling.support.CronExpression.isValidExpression(cronExpression)) {
            throw new IllegalArgumentException("Cron 表達式格式無效");
        }

        SysScheduledTask existing = sysScheduledTaskMapper.selectByTaskKey(cronDTO.getTaskKey());
        if (existing == null) {
            throw new IllegalArgumentException("找不到定時任務: " + cronDTO.getTaskKey());
        }

        int rows = sysScheduledTaskMapper.updateCronExpression(cronDTO.getTaskKey(), cronExpression);
        if (rows > 0) {
            log.info("定時器任務 {} 的 Cron 已更新為 {}", cronDTO.getTaskKey(), cronExpression);
        }
        return rows;
    }
}
