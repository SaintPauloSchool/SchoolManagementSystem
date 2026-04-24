package com.sms.system.entity.vo;

import java.io.Serializable;

/**
 * 发送统计 VO
 *
 */
public class SendStatisticsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;

    public SendStatisticsVO() {}

    public SendStatisticsVO(Integer totalCount, Integer successCount, Integer failCount) {
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.failCount = failCount;
    }

    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }

    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }

    public Integer getFailCount() { return failCount; }
    public void setFailCount(Integer failCount) { this.failCount = failCount; }
}
