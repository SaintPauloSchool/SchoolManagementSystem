package com.sms.system.entity.vo;

import java.io.Serializable;

/**
 * 學生匹配單次操作結果（綁定、清除、刪除、數據對照等）
 */
public class SysStudentMatchOperationResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private Integer affectedCount;

    public static SysStudentMatchOperationResultVO success(String message) {
        SysStudentMatchOperationResultVO result = new SysStudentMatchOperationResultVO();
        result.setSuccess(true);
        result.setMessage(message);
        return result;
    }

    public static SysStudentMatchOperationResultVO success(String message, int affectedCount) {
        SysStudentMatchOperationResultVO result = success(message);
        result.setAffectedCount(affectedCount);
        return result;
    }

    public static SysStudentMatchOperationResultVO failure(String message) {
        SysStudentMatchOperationResultVO result = new SysStudentMatchOperationResultVO();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getAffectedCount() {
        return affectedCount;
    }

    public void setAffectedCount(Integer affectedCount) {
        this.affectedCount = affectedCount;
    }
}
