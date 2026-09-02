package com.sms.scheduler;

import com.sms.system.service.IScheduledTaskExecutionLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * 統一定時/手動任務調度入口，集中處理執行鎖與手動觸發標記。
 */
@Component
public class ScheduledTaskInvoker {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskInvoker.class);

    private static final String DEFAULT_METHOD_NAME = "executeTask";

    private final ApplicationContext applicationContext;
    private final ScheduledTaskSupport scheduledTaskSupport;
    private final IScheduledTaskExecutionLockService executionLockService;

    public ScheduledTaskInvoker(ApplicationContext applicationContext,
                                ScheduledTaskSupport scheduledTaskSupport,
                                IScheduledTaskExecutionLockService executionLockService) {
        this.applicationContext = applicationContext;
        this.scheduledTaskSupport = scheduledTaskSupport;
        this.executionLockService = executionLockService;
    }

    /**
     * @param manual true 表示手動觸發（不受 enabled 限制）
     */
    public void invoke(String beanName, String methodName, String taskKey, boolean manual) {
        invoke(beanName, methodName, taskKey, manual, false);
    }

    /**
     * @param lockAlreadyHeld true 表示調用方已持有執行鎖（手動觸發場景）
     */
    public void invoke(String beanName, String methodName, String taskKey, boolean manual, boolean lockAlreadyHeld) {
        if (!StringUtils.hasText(beanName) || !StringUtils.hasText(taskKey)) {
            log.warn("任務調度參數不完整，beanName={}, taskKey={}", beanName, taskKey);
            return;
        }
        String resolvedMethod = StringUtils.hasText(methodName) ? methodName : DEFAULT_METHOD_NAME;

        if (!manual && scheduledTaskSupport.shouldSkipForSchedule(taskKey)) {
            return;
        }
        boolean lockAcquired = false;
        if (lockAlreadyHeld) {
            lockAcquired = true;
        } else if (!executionLockService.tryAcquire(taskKey)) {
            return;
        } else {
            lockAcquired = true;
        }

        if (manual) {
            scheduledTaskSupport.markManualTrigger();
        }
        try {
            Object bean = applicationContext.getBean(beanName);
            Method method = bean.getClass().getDeclaredMethod(resolvedMethod);
            method.setAccessible(true);
            method.invoke(bean);
        } catch (NoSuchBeanDefinitionException e) {
            log.error("執行任務 {} 失敗，找不到 Bean: {}", taskKey, beanName, e);
        } catch (NoSuchMethodException e) {
            log.error("執行任務 {} 失敗，找不到方法: {}", taskKey, resolvedMethod, e);
        } catch (Exception e) {
            log.error("執行任務 {} 異常", taskKey, e);
        } finally {
            if (manual) {
                scheduledTaskSupport.clearManualTrigger();
            }
            if (lockAcquired) {
                executionLockService.release(taskKey);
            }
        }
    }
}
