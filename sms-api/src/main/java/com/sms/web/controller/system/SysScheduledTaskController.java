package com.sms.web.controller.system;

import com.sms.common.annotation.Log;
import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.enums.BusinessType;
import com.sms.system.entity.dto.SysScheduledTaskStatusDTO;
import com.sms.system.entity.vo.SysScheduledTaskVO;
import com.sms.system.service.ISysScheduledTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 定時任務配置 Controller
 */
@RestController
@RequestMapping("/system/scheduledTask")
public class SysScheduledTaskController extends BaseController {

    @Autowired
    private ISysScheduledTaskService sysScheduledTaskService;

    @Log(title = "查詢定時任務配置", businessType = BusinessType.SELECT)
    @GetMapping("/list")
    public AjaxResult list() {
        List<SysScheduledTaskVO> list = sysScheduledTaskService.selectTaskList();
        return AjaxResult.success(list);
    }

    @Log(title = "更新定時任務啟用狀態", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public AjaxResult updateStatus(@RequestBody SysScheduledTaskStatusDTO statusDTO) {
        try {
            return toAjax(sysScheduledTaskService.updateEnabled(statusDTO));
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
