package com.sms.system.service.impl;

import com.sms.system.entity.SysScheduledTask;
import com.sms.system.entity.SysTaskLog;
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

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    /** taskKey -> enabled */
    private final Map<String, Boolean> enabledCache = new ConcurrentHashMap<>();

    @PostConstruct
    @Override
    public void refreshCache() {
        List<SysScheduledTaskVO> tasks = sysScheduledTaskMapper.selectTaskList();
        if (tasks == null || tasks.isEmpty()) {
            enabledCache.clear();
            log.warn("定時任務配置為空，請確認 sys_scheduled_task 表已初始化");
            return;
        }
        Map<String, Boolean> refreshed = new ConcurrentHashMap<>();
        for (SysScheduledTaskVO task : tasks) {
            if (task == null || !StringUtils.hasText(task.getTaskKey())) {
                continue;
            }
            refreshed.put(task.getTaskKey(), "1".equals(task.getEnabled()));
        }
        enabledCache.clear();
        enabledCache.putAll(refreshed);
        log.info("定時任務啟用狀態緩存已刷新，共 {} 項", enabledCache.size());
    }

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
            enabledCache.remove(taskKey);
            return false;
        }
        boolean enabled = "1".equals(task.getEnabled());
        enabledCache.put(taskKey, enabled);
        return enabled;
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
            enabledCache.put(statusDTO.getTaskKey(), "1".equals(enabled));
            log.info("定時任務 {} 已{}", statusDTO.getTaskKey(), "1".equals(enabled) ? "啟用" : "停用");
        }
        return rows;
    }
}
