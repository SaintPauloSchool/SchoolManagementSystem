package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 手動執行定時任務請求
 */
public class SysTaskExecuteDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String beanName;
    private String methodName;

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
