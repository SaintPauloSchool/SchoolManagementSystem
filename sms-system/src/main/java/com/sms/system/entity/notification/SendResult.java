package com.sms.system.entity.notification;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
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

    /** 发送失败的用户 ID 与失败原因的映射 */
    private final Map<String, String> failedUserReasons;

    public SendResult(int successCount, int failCount, Set<String> successUserIds) {
        this(successCount, failCount, successUserIds, null);
    }

    public SendResult(int successCount, int failCount, Set<String> successUserIds, Map<String, String> failedUserReasons) {
        this.successCount = successCount;
        this.failCount = failCount;
        this.successUserIds = successUserIds != null ? successUserIds : Collections.emptySet();
        this.failedUserReasons = failedUserReasons != null ? failedUserReasons : Collections.emptyMap();
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

    /**
     * 获取发送失败的用户 ID 及原因的映射
     *
     * @return 失败用户及原因映射（不可为 null）
     */
    public Map<String, String> getFailedUserReasons() {
        return failedUserReasons;
    }
}
