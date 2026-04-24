package com.sms.system.entity.notification;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

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

    /** 发送成功的用户 ID 集合（家长 + 学生） */
    private final Set<String> successUserIds;

    public SendResult(int successCount, int failCount, Set<String> successUserIds) {
        this.successCount = successCount;
        this.failCount = failCount;
        this.successUserIds = successUserIds != null ? successUserIds : Collections.emptySet();
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    /**
     * 获取发送成功的用户 ID 集合
     *
     * @return 成功用户 ID 集合（不可为 null）
     */
    public Set<String> getSuccessUserIds() {
        return successUserIds;
    }
}
