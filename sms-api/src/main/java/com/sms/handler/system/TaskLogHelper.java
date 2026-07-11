package com.sms.handler.system;

import com.sms.system.entity.dto.SysTaskLogInsertDTO;
import com.sms.system.entity.task.TaskResult;
import com.sms.system.service.ISysTaskLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.Callable;

/**
 * 定時任務執行日誌記錄輔助類
 */
@Component
public class TaskLogHelper {

    private static final Logger log = LoggerFactory.getLogger(TaskLogHelper.class);

    @Autowired
    private ISysTaskLogService sysTaskLogService;

    /**
     * 執行任務並記錄日誌
     *
     * @param taskName   任務名稱
     * @param beanName   Bean名稱
     * @param methodName 方法名稱
     * @param task       要執行的任務邏輯
     */
    public void executeAndLog(String taskName, String beanName, String methodName, Callable<TaskResult> task) {
        Date startTime = new Date();
        long start = System.currentTimeMillis();
        String status = "0";
        String failReason = null;
        int successCount = 0;
        int failCount = 0;

        try {
            log.info("開始執行定時任務: {}", taskName);
            TaskResult result = task.call();
            if (result != null) {
                successCount = result.getSuccessCount();
                failCount = result.getFailCount();
                failReason = result.getMessage();

                if (failCount > 0 && successCount > 0) {
                    status = "2";
                } else if (failCount > 0 && successCount == 0) {
                    status = "1";
                } else {
                    status = "0";
                }
            }
            log.info("定時任務: {} 執行結束, 狀態: {}, 成功: {}, 失敗: {}", taskName, status, successCount, failCount);
        } catch (Exception e) {
            status = "1";
            failReason = getExceptionMessage(e);
            log.error("定時任務: {} 執行失敗: {}", taskName, failReason, e);
        } finally {
            long duration = System.currentTimeMillis() - start;
            saveLog(taskName, beanName, methodName, status, failReason, successCount, failCount, startTime, duration);
        }
    }

    private void saveLog(String taskName, String beanName, String methodName, String status, String failReason,
                         int successCount, int failCount, Date executionTime, long duration) {
        try {
            SysTaskLogInsertDTO sysTaskLogInsertDTO = new SysTaskLogInsertDTO();
            sysTaskLogInsertDTO.setTaskName(taskName);
            sysTaskLogInsertDTO.setBeanName(beanName);
            sysTaskLogInsertDTO.setMethodName(methodName);
            sysTaskLogInsertDTO.setStatus(status);

            if ("0".equals(status)) {
                sysTaskLogInsertDTO.setIsProcessed("1");
            } else {
                sysTaskLogInsertDTO.setIsProcessed("0");
            }

            sysTaskLogInsertDTO.setFailReason(failReason);
            sysTaskLogInsertDTO.setSuccessCount(successCount);
            sysTaskLogInsertDTO.setFailCount(failCount);
            sysTaskLogInsertDTO.setExecutionTime(executionTime);
            sysTaskLogInsertDTO.setDuration(duration);

            sysTaskLogService.insertTaskLog(sysTaskLogInsertDTO);
        } catch (Exception e) {
            log.error("保存定時任務日誌失敗", e);
        }
    }

    private String getExceptionMessage(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : e.toString();
        if (msg.length() > 1900) {
            msg = msg.substring(0, 1900) + "...";
        }
        return msg;
    }
}
