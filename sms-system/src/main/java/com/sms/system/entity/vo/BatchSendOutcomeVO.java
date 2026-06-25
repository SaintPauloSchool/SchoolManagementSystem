package com.sms.system.entity.vo;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 單批家校通知發送結果 VO。
 * <p>用於解析企微批量接口返回後，統計本批成功用戶、失敗人數及失敗原因。</p>
 */
public class BatchSendOutcomeVO {

    /** 本批發送成功的用戶 ID 集合（家長 + 學生） */
    private final Set<String> successUserIds;

    /** 本批發送失敗人數 */
    private final int failCount;

    /** 本批失敗用戶 ID 與失敗原因的映射 */
    private final Map<String, String> failedUserReasons;

    public BatchSendOutcomeVO(Set<String> successUserIds, int failCount, Map<String, String> failedUserReasons) {
        this.successUserIds = successUserIds != null ? successUserIds : Collections.emptySet();
        this.failCount = failCount;
        this.failedUserReasons = failedUserReasons != null ? failedUserReasons : Collections.emptyMap();
    }

    public Set<String> getSuccessUserIds() {
        return successUserIds;
    }

    public int getFailCount() {
        return failCount;
    }

    public Map<String, String> getFailedUserReasons() {
        return failedUserReasons;
    }
}
