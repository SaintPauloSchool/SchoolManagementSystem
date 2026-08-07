package com.sms.system.service;

import com.sms.system.entity.dto.SysScheduledTaskCronDTO;
import com.sms.system.entity.dto.SysScheduledTaskStatusDTO;
import com.sms.system.entity.vo.SysScheduledTaskVO;

import java.util.List;

/**
 * 定時任務配置 Service
 */
public interface ISysScheduledTaskService {

    /**
     * 查詢所有定時任務配置（含最近執行記錄）
     */
    List<SysScheduledTaskVO> selectTaskList();

    /**
     * 判斷定時任務是否啟用
     */
    boolean isEnabled(String taskKey);

    /**
     * 更新定時任務啟用狀態
     */
    int updateEnabled(SysScheduledTaskStatusDTO statusDTO);

    /**
     * 更新定時任務 Cron 表達式
     */
    int updateCronExpression(SysScheduledTaskCronDTO cronDTO);
}
