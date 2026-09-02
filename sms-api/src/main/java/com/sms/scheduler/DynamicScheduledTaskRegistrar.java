package com.sms.scheduler;

import com.sms.system.entity.SysScheduledTask;
import com.sms.system.mapper.SysScheduledTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 從資料庫讀取 Cron 配置，動態註冊定時任務。
 */
@Component
public class DynamicScheduledTaskRegistrar {

    private static final Logger log = LoggerFactory.getLogger(DynamicScheduledTaskRegistrar.class);

    private static final String DEFAULT_METHOD_NAME = "executeTask";

    private final SysScheduledTaskMapper sysScheduledTaskMapper;
    private final ScheduledTaskInvoker scheduledTaskInvoker;

    private final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public DynamicScheduledTaskRegistrar(SysScheduledTaskMapper sysScheduledTaskMapper,
                                         ScheduledTaskInvoker scheduledTaskInvoker) {
        this.sysScheduledTaskMapper = sysScheduledTaskMapper;
        this.scheduledTaskInvoker = scheduledTaskInvoker;
    }

    @PostConstruct
    public void init() {
        taskScheduler.setPoolSize(8);
        taskScheduler.setThreadNamePrefix("dynamic-scheduled-");
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.setAwaitTerminationSeconds(30);
        taskScheduler.initialize();
        rescheduleAll();
    }

    @PreDestroy
    public void destroy() {
        cancelAll();
        taskScheduler.shutdown();
    }

    public void rescheduleAll() {
        cancelAll();
        List<SysScheduledTask> tasks = sysScheduledTaskMapper.selectAllTasks();
        if (tasks == null || tasks.isEmpty()) {
            log.warn("未找到定時任務配置，跳過動態調度註冊");
            return;
        }
        for (SysScheduledTask task : tasks) {
            registerTask(task);
        }
        log.info("動態定時任務已註冊 {} 項", scheduledTasks.size());
    }

    public void reschedule(String taskKey) {
        if (!StringUtils.hasText(taskKey)) {
            return;
        }
        cancel(taskKey);
        SysScheduledTask task = sysScheduledTaskMapper.selectByTaskKey(taskKey);
        if (task == null) {
            log.warn("重新調度失敗，找不到任務 taskKey={}", taskKey);
            return;
        }
        registerTask(task);
        log.info("定時任務 {} 已重新調度，Cron={}，enabled={}", taskKey, task.getCronExpression(), task.getEnabled());
    }

    private void registerTask(SysScheduledTask task) {
        if (task == null || !StringUtils.hasText(task.getTaskKey())) {
            return;
        }
        if (!"1".equals(task.getEnabled())) {
            log.debug("任務 {} 已停用，跳過註冊", task.getTaskKey());
            return;
        }
        String cron = task.getCronExpression();
        if (!isValidCron(cron)) {
            log.warn("任務 {} 的 Cron 表達式無效，跳過註冊: {}", task.getTaskKey(), cron);
            return;
        }
        String taskKey = task.getTaskKey();
        cancel(taskKey);
        String beanName = task.getTaskBean();
        String methodName = StringUtils.hasText(task.getMethodName()) ? task.getMethodName() : DEFAULT_METHOD_NAME;
        Runnable runnable = () -> scheduledTaskInvoker.invoke(beanName, methodName, taskKey, false);
        Trigger trigger = triggerContext -> {
            SysScheduledTask current = sysScheduledTaskMapper.selectByTaskKey(taskKey);
            if (current == null) {
                log.warn("任務 {} 不存在，停止調度", taskKey);
                return null;
            }
            if (!"1".equals(current.getEnabled())) {
                return null;
            }
            String currentCron = current.getCronExpression();
            if (!isValidCron(currentCron)) {
                log.warn("任務 {} 的 Cron 表達式無效，停止調度: {}", taskKey, currentCron);
                return null;
            }
            return new CronTrigger(currentCron.trim()).nextExecutionTime(triggerContext);
        };
        try {
            ScheduledFuture<?> future = taskScheduler.schedule(runnable, trigger);
            scheduledTasks.put(taskKey, future);
            log.info("已註冊定時任務 {}，Cron={}", taskKey, cron.trim());
        } catch (Exception e) {
            log.error("註冊定時任務 {} 失敗，Cron={}", taskKey, cron, e);
        }
    }

    private void cancelAll() {
        scheduledTasks.forEach((key, future) -> {
            if (future != null) {
                future.cancel(true);
            }
        });
        scheduledTasks.clear();
    }

    private void cancel(String taskKey) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskKey);
        if (future != null) {
            future.cancel(true);
            log.info("已取消定時任務調度 taskKey={}", taskKey);
        }
    }

    public static boolean isValidCron(String cronExpression) {
        if (!StringUtils.hasText(cronExpression)) {
            return false;
        }
        return CronExpression.isValidExpression(cronExpression.trim());
    }
}
