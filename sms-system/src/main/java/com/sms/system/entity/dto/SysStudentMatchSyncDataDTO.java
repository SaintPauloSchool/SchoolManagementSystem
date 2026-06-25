package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 學籍與企微自動比對請求
 */
public class SysStudentMatchSyncDataDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 操作人（由控制層注入） */
    private String operName;

    public String getOperName() {
        return operName;
    }

    public void setOperName(String operName) {
        this.operName = operName;
    }
}
