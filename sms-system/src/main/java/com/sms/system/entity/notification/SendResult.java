package com.sms.system.entity.notification;

import java.io.Serializable;

/**
 * 通知发送结果实体类
 *
 */
public class SendResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 发送成功人数 */
    private final int successCount;

    /** 发送失败人数 */
    private final int failCount;

    public SendResult(int successCount, int failCount) {
        this.successCount = successCount;
        this.failCount = failCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailCount() {
        return failCount;
    }
}
