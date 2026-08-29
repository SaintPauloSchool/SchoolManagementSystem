package com.sms.system.entity.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 批量新增用戶角色結果 VO
 */
public class SysUserRoleBatchInsertResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int successCount;
    private List<String> skipped;
    private String message;

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public List<String> getSkipped() {
        return skipped;
    }

    public void setSkipped(List<String> skipped) {
        this.skipped = skipped;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
