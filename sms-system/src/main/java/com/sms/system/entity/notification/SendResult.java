package com.sms.system.entity.notification;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 通知發送結果實體類
 *
 */
public class SendResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 發送成功人數 */
    private final int successCount;

    /** 發送失敗人數 */
    private final int failCount;

    /** 發送成功的用戶 ID 集合（家長 + 學生） */
    private final Set<String> successUserIds;

    /** 發送失敗的用戶 ID 與失敗原因的映射 */
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
     * 獲取發送成功的用戶 ID 集合
     *
     * @return 成功用戶 ID 集合（不可爲 null）
     */
    public Set<String> getSuccessUserIds() {
        return successUserIds;
    }

    /**
     * 獲取發送失敗的用戶 ID 及原因的映射
     *
     * @return 失敗用戶及原因映射（不可爲 null）
     */
    public Map<String, String> getFailedUserReasons() {
        return failedUserReasons;
    }
}
