package com.sms.web.controller.system;

import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.core.page.TableDataInfo;
import com.sms.system.entity.SysTaskLog;
import com.sms.system.service.ISysTaskLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

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

    /**
     * 查詢定時任務日誌列表
     */
    @GetMapping("/list")
    public TableDataInfo list(SysTaskLog sysTaskLog) {
        startPage();
        List<SysTaskLog> list = sysTaskLogService.selectTaskLogList(sysTaskLog);
        return getDataTable(list);
    }

    /**
     * 手動執行定時任務
     */
    @PostMapping("/execute")
    public AjaxResult executeTask(@RequestBody Map<String, String> params) {
        String beanName = params.get("beanName");
        String methodName = params.get("methodName");

        if (beanName == null || methodName == null) {
            return AjaxResult.error("參數錯誤：beanName 或 methodName 不能為空");
        }

        try {
            // 從 Spring 容器獲取 Bean
            Object bean = applicationContext.getBean(beanName);

            if (bean == null) {
                return AjaxResult.error("找不到對應的處理器: " + beanName);
            }

            // 反射獲取方法（這裡假設任務方法都沒有參數）
            Method method = bean.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            
            log.info("手動執行任務開始 - Bean: {}, Method: {}", beanName, methodName);

            method.invoke(bean);
            
            return AjaxResult.success("任務觸發成功，請稍後查看日誌");
        } catch (NoSuchMethodException e) {
            log.error("手動執行任務失敗，找不到方法", e);
            return AjaxResult.error("找不到對應的方法: " + methodName);
        } catch (Exception e) {
            log.error("手動執行任務異常", e);
            return AjaxResult.error("任務執行異常: " + e.getMessage());
        }
    }
}
