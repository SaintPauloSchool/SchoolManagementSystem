package com.sms.system.entity.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 批量新增管理員結果 VO
 */
public class SysAdminBatchInsertResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer successCount;
    private List<String> skipped;
    private String message;

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
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
