package com.sms.web.controller.system;

import com.sms.common.annotation.Log;
import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.enums.BusinessType;
import com.sms.scheduler.ScheduledTaskSupport;
import com.sms.system.entity.dto.SysTaskExecuteDTO;
import com.sms.system.entity.dto.SysTaskLogQueryDTO;
import com.sms.system.entity.dto.SysTaskLogUpdateDTO;
import com.sms.system.entity.vo.SysTaskLogVO;
import com.sms.system.service.ISysTaskLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
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
    private ApplicationContext applicationContext;

    @Autowired
    private ScheduledTaskSupport scheduledTaskSupport;

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
            Object bean = applicationContext.getBean(beanName);

            Method method = bean.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);

            log.info("手動執行任務開始 - Bean: {}, Method: {}", beanName, methodName);

            scheduledTaskSupport.markManualTrigger();
            try {
                method.invoke(bean);
            } finally {
                scheduledTaskSupport.clearManualTrigger();
            }

            return AjaxResult.success("任務觸發成功，請稍後查看日誌");
        } catch (NoSuchBeanDefinitionException e) {
            log.error("手動執行任務失敗，找不到 Bean: {}", beanName, e);
            return AjaxResult.error("找不到對應的處理器: " + beanName);
        } catch (NoSuchMethodException e) {
            log.error("手動執行任務失敗，找不到方法", e);
            return AjaxResult.error("找不到對應的方法: " + methodName);
        } catch (Exception e) {
            log.error("手動執行任務異常", e);
            return AjaxResult.error("任務執行異常: " + e.getMessage());
        }
    }

    @Log(title = "修改定時任務日誌", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysTaskLogUpdateDTO sysTaskLogUpdateDTO) {
        return toAjax(sysTaskLogService.updateTaskLog(sysTaskLogUpdateDTO));
    }
}
