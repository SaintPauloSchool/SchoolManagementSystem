package com.sms.scheduler;

import com.sms.system.entity.SysScheduledTask;
import com.sms.system.mapper.SysScheduledTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.Method;
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

    private final ApplicationContext applicationContext;
    private final SysScheduledTaskMapper sysScheduledTaskMapper;

    private final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public DynamicScheduledTaskRegistrar(ApplicationContext applicationContext,
                                         SysScheduledTaskMapper sysScheduledTaskMapper) {
        this.applicationContext = applicationContext;
        this.sysScheduledTaskMapper = sysScheduledTaskMapper;
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
    }

    private void registerTask(SysScheduledTask task) {
        if (task == null || !StringUtils.hasText(task.getTaskKey())) {
            return;
        }
        String cron = task.getCronExpression();
        if (!isValidCron(cron)) {
            log.warn("任務 {} 的 Cron 表達式無效，跳過註冊: {}", task.getTaskKey(), cron);
            return;
        }
        String beanName = task.getTaskBean();
        String methodName = StringUtils.hasText(task.getMethodName()) ? task.getMethodName() : DEFAULT_METHOD_NAME;
        Runnable runnable = () -> invokeTask(beanName, methodName, task.getTaskKey());
        try {
            ScheduledFuture<?> future = taskScheduler.schedule(runnable, new CronTrigger(cron.trim()));
            scheduledTasks.put(task.getTaskKey(), future);
            log.info("已註冊定時任務 {}，Cron={}", task.getTaskKey(), cron.trim());
        } catch (Exception e) {
            log.error("註冊定時任務 {} 失敗，Cron={}", task.getTaskKey(), cron, e);
        }
    }

    private void invokeTask(String beanName, String methodName, String taskKey) {
        try {
            Object bean = applicationContext.getBean(beanName);
            Method method = bean.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(bean);
        } catch (NoSuchBeanDefinitionException e) {
            log.error("執行定時任務 {} 失敗，找不到 Bean: {}", taskKey, beanName, e);
        } catch (NoSuchMethodException e) {
            log.error("執行定時任務 {} 失敗，找不到方法: {}", taskKey, methodName, e);
        } catch (Exception e) {
            log.error("執行定時任務 {} 異常", taskKey, e);
        }
    }

    private void cancelAll() {
        scheduledTasks.forEach((key, future) -> {
            if (future != null) {
                future.cancel(false);
            }
        });
        scheduledTasks.clear();
    }

    private void cancel(String taskKey) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskKey);
        if (future != null) {
            future.cancel(false);
        }
    }

    public static boolean isValidCron(String cronExpression) {
        if (!StringUtils.hasText(cronExpression)) {
            return false;
        }
        return CronExpression.isValidExpression(cronExpression.trim());
    }
}
