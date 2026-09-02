package com.sms.web.controller.system;

import com.sms.common.annotation.Log;
import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.enums.BusinessType;
import com.sms.scheduler.ScheduledTaskInvoker;
import com.sms.system.service.IScheduledTaskExecutionLockService;
import com.sms.system.entity.dto.SysTaskExecuteDTO;
import com.sms.system.entity.dto.SysTaskLogQueryDTO;
import com.sms.system.entity.dto.SysTaskLogUpdateDTO;
import com.sms.system.entity.vo.SysTaskLogVO;
import com.sms.system.mapper.SysScheduledTaskMapper;
import com.sms.system.service.ISysTaskLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 定時任務執行日誌 Controller
 */
@RestController
@RequestMapping("/system/taskLog")
public class SysTaskLogController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(SysTaskLogController.class);

    @Autowired
    private ISysTaskLogService sysTaskLogService;

    @Autowired
    private SysScheduledTaskMapper sysScheduledTaskMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ScheduledTaskInvoker scheduledTaskInvoker;

    @Autowired
    private IScheduledTaskExecutionLockService executionLockService;

    @Autowired
    @Qualifier("threadPoolTaskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Log(title = "查詢定時任務日誌", businessType = BusinessType.SELECT)
    @GetMapping("/list")
    public TableDataInfo list(SysTaskLogQueryDTO sysTaskLogQueryDTO) {
        startPage();
        List<SysTaskLogVO> sysTaskLogList = sysTaskLogService.selectTaskLogList(sysTaskLogQueryDTO);
        return getDataTable(sysTaskLogList);
    }

    @Log(title = "手動執行定時任務", businessType = BusinessType.OTHER)
    @PostMapping("/execute")
    public AjaxResult executeTask(@RequestBody SysTaskExecuteDTO sysTaskExecuteDTO) {
        String beanName = sysTaskExecuteDTO.getBeanName();
        String methodName = sysTaskExecuteDTO.getMethodName();

        if (beanName == null || methodName == null) {
            return AjaxResult.error("參數錯誤：beanName 或 methodName 不能為空");
        }

        try {
            applicationContext.getBean(beanName);
        } catch (NoSuchBeanDefinitionException e) {
            log.error("手動執行任務失敗，找不到 Bean: {}", beanName, e);
            return AjaxResult.error("找不到對應的處理器: " + beanName);
        }

        String taskKey = sysScheduledTaskMapper.selectTaskKeyByTaskBean(beanName);
        if (taskKey == null) {
            return AjaxResult.error("找不到對應的定時任務配置: " + beanName);
        }

        if (!executionLockService.tryAcquire(taskKey)) {
            return AjaxResult.error("任務正在執行中，請稍後再試");
        }

        log.info("手動執行任務已提交 - Bean: {}, Method: {}, taskKey: {}", beanName, methodName, taskKey);

        try {
            threadPoolTaskExecutor.execute(() ->
                    scheduledTaskInvoker.invoke(beanName, methodName, taskKey, true, true));
        } catch (Exception e) {
            executionLockService.release(taskKey);
            log.error("手動執行任務提交失敗 - Bean: {}, Method: {}", beanName, methodName, e);
            return AjaxResult.error("任務提交失敗，請稍後再試");
        }

        return AjaxResult.success("任務已提交執行，請稍後查看日誌");
    }

    @Log(title = "修改定時任務日誌", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysTaskLogUpdateDTO sysTaskLogUpdateDTO) {
        return toAjax(sysTaskLogService.updateTaskLog(sysTaskLogUpdateDTO));
    }
}
