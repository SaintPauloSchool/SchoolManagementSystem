package com.sms.system.entity.vo;

import java.io.Serializable;

/**
 * 閱讀統計 VO
 *
 */
public class ReadStatisticsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer readCount;
    private Integer replyCount;

    public ReadStatisticsVO() {}

    public ReadStatisticsVO(Integer readCount, Integer replyCount) {
        this.readCount = readCount;
        this.replyCount = replyCount;
    }

    public Integer getReadCount() { return readCount; }
    public void setReadCount(Integer readCount) { this.readCount = readCount; }

    public Integer getReplyCount() { return replyCount; }
    public void setReplyCount(Integer replyCount) { this.replyCount = replyCount; }
}
