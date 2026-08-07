package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 定時任務啟用狀態更新 DTO
 */
public class SysScheduledTaskStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskKey;
    /** 是否啟用（0停用 1啟用） */
    private String enabled;

    public String getTaskKey() {
        return taskKey;
    }

    public void setTaskKey(String taskKey) {
        this.taskKey = taskKey;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }
}
