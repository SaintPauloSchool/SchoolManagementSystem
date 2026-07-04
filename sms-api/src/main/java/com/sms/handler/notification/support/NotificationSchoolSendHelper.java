package com.sms.handler.notification.support;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.framework.wechat.WechatWorkHttpClient;
import com.sms.system.entity.notification.Notification;
import com.sms.system.entity.notification.SendResult;
import com.sms.system.entity.vo.ParentStudentMessageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 企業微信家校通知發送輔助類。
 * <p>所有家校通知僅通過 {@code to_parent_userid} 發送給家長。</p>
 */
@Component
public class NotificationSchoolSendHelper {

    private static final Logger log = LoggerFactory.getLogger(NotificationSchoolSendHelper.class);

    /** 家長 userid 每批上限（企微接口限制 1000） */
    public static final int PARENT_BATCH_SIZE = 1000;
    /** 重複消息檢查間隔（秒） */
    private static final int DUPLICATE_CHECK_INTERVAL = 1800;
    /** 個性化發送時並行執行緒池大小 */
    private static final int PERSONALIZED_SEND_POOL_SIZE = 20;

    @Value("${wechat.work.agentId:1000033}")
    private Integer agentId;

    @Autowired
    private WechatWorkHttpClient wechatWorkHttpClient;

    @Autowired
    private NotificationMessageContentHelper messageContentHelper;

    /**
     * 按家長-學生綁定逐條發送個性化家校通知（僅 {@code to_parent_userid}）。
     * <p>若 {@code messageInfos} 為空，降級為 {@link #sendInBatches} 統一正文批量發送。</p>
     */
    public SendResult sendWithPersonalization(Notification notification,
                                              List<String> parentUserIds,
                                              List<ParentStudentMessageInfo> messageInfos) {
        if (messageInfos == null || messageInfos.isEmpty()) {
            return sendInBatches(notification, parentUserIds);
        }

        Map<String, List<ParentStudentMessageInfo>> parentToMessagesMap = messageInfos.stream()
                .collect(Collectors.groupingBy(ParentStudentMessageInfo::getParentUserId));

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        Set<String> successUserIds = Collections.synchronizedSet(new HashSet<>());
        Map<String, String> failedUserReasons = new ConcurrentHashMap<>();

        ForkJoinPool customThreadPool = new ForkJoinPool(PERSONALIZED_SEND_POOL_SIZE);
        try {
            customThreadPool.submit(() -> parentToMessagesMap.entrySet().parallelStream().forEach(entry -> {
                String parentUserId = entry.getKey();
                for (ParentStudentMessageInfo msgInfo : entry.getValue()) {
                    try {
                        String content = messageContentHelper.buildPublishContent(
                                notification, msgInfo.getClassName(), msgInfo.getStudentName(),
                                msgInfo.getStudentUserId());
                        JSONObject result = wechatWorkHttpClient.sendSchoolNotification(
                                buildParentOnlyPayload(parentUserId, content));
                        if (isWechatSuccess(result)) {
                            successCount.incrementAndGet();
                            successUserIds.add(parentUserId);
                            log.debug("成功發送通知給家長 {}", parentUserId);
                        } else {
                            failCount.incrementAndGet();
                            failedUserReasons.put(parentUserId, wechatErrorReason(result));
                            log.error("發送通知給家長 {} 失敗: code={}, msg={}", parentUserId,
                                    result.getInteger("errcode"), result.getString("errmsg"));
                        }
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        failedUserReasons.put(parentUserId, "發送異常: " + e.getMessage());
                        log.error("發送通知給家長 {} 異常", parentUserId, e);
                    }
                }
            })).get();
        } catch (Exception e) {
            log.error("自定義執行緒池發送批量通知時發生異常", e);
        } finally {
            customThreadPool.shutdown();
        }

        // 無個性化信息的接收人（如自定義家校）走統一正文
        List<String> genericParentIds = collectParentsWithoutPersonalization(parentUserIds, parentToMessagesMap);
        if (!genericParentIds.isEmpty()) {
            SendResult genericResult = sendInBatches(notification, genericParentIds);
            mergeSendResult(successCount, failCount, successUserIds, failedUserReasons, genericResult);
        }

        log.info("通知 {} 已全部發送完成，成功: {}, 失敗: {}",
                notification.getNotificationId(), successCount.get(), failCount.get());
        return new SendResult(successCount.get(), failCount.get(), successUserIds, failedUserReasons);
    }

    private List<String> collectParentsWithoutPersonalization(List<String> parentUserIds,
                                                                Map<String, List<ParentStudentMessageInfo>> parentToMessagesMap) {
        if (parentUserIds == null || parentUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> seen = new HashSet<>();
        List<String> missing = new ArrayList<>();
        for (String parentUserId : parentUserIds) {
            if (!StringUtils.hasText(parentUserId) || !seen.add(parentUserId)) {
                continue;
            }
            if (!parentToMessagesMap.containsKey(parentUserId)) {
                missing.add(parentUserId);
            }
        }
        return missing;
    }

    private void mergeSendResult(AtomicInteger successCount,
                                 AtomicInteger failCount,
                                 Set<String> successUserIds,
                                 Map<String, String> failedUserReasons,
                                 SendResult other) {
        if (other == null) {
            return;
        }
        successCount.addAndGet(other.getSuccessCount());
        failCount.addAndGet(other.getFailCount());
        if (other.getSuccessUserIds() != null) {
            successUserIds.addAll(other.getSuccessUserIds());
        }
        if (other.getFailedUserReasons() != null) {
            failedUserReasons.putAll(other.getFailedUserReasons());
        }
    }

    /** 使用通知統一正文，按企微接口上限分批發送給家長。 */
    public SendResult sendInBatches(Notification notification, List<String> parentUserIds) {
        return sendInBatches(notification.getNotificationId(), parentUserIds,
                nId -> messageContentHelper.buildPublishContent(notification));
    }

    private SendResult sendInBatches(Long notificationId,
                                     List<String> parentUserIds,
                                     Function<Long, String> contentSupplier) {
        if (parentUserIds == null || parentUserIds.isEmpty()) {
            return new SendResult(0, 0, Collections.emptySet(), Collections.emptyMap());
        }

        int totalBatches = calcBatchCount(parentUserIds.size(), PARENT_BATCH_SIZE);
        log.info("通知 {} 需要分 {} 批發送給家長", notificationId, totalBatches);

        int successCount = 0;
        int failCount = 0;
        Set<String> successUserIds = new HashSet<>();
        Map<String, String> failedUserReasons = new HashMap<>();

        for (int i = 0; i < totalBatches; i++) {
            List<String> batch = extractBatch(parentUserIds, i, PARENT_BATCH_SIZE);
            if (batch.isEmpty()) {
                continue;
            }

            JSONObject payload = buildParentOnlyPayload(batch, contentSupplier.apply(notificationId));
            log.info("發送通知 {} 的第 {}/{} 批，家長: {}", notificationId, i + 1, totalBatches, batch.size());

            JSONObject result = wechatWorkHttpClient.sendSchoolNotification(payload);
            if (!isWechatSuccess(result)) {
                String reason = wechatErrorReason(result);
                log.error("通知 {} 第 {} 批發送失敗: code={}, msg={}",
                        notificationId, i + 1, result.getInteger("errcode"), result.getString("errmsg"));
                failCount += batch.size();
                markUsersFailed(batch, reason, failedUserReasons);
                continue;
            }

            log.info("通知 {} 第 {}/{} 批發送成功", notificationId, i + 1, totalBatches);
            Set<String> batchSuccessUsers = new HashSet<>(batch);
            removeInvalidUsers(batchSuccessUsers, result);
            int batchFailCount = batch.size() - batchSuccessUsers.size();
            if (batchFailCount > 0) {
                log.warn("通知 {} 第 {} 批有 {} 個無效家長", notificationId, i + 1, batchFailCount);
                for (String parentId : batch) {
                    if (!batchSuccessUsers.contains(parentId)) {
                        failedUserReasons.put(parentId, "無效用戶或微信端未關注");
                    }
                }
            }
            failCount += batchFailCount;
            successCount += batchSuccessUsers.size();
            successUserIds.addAll(batchSuccessUsers);
        }

        log.info("通知 {} 已全部發送完成，共 {} 批，成功: {}, 失敗: {}",
                notificationId, totalBatches, successCount, failCount);
        return new SendResult(successCount, failCount, successUserIds, failedUserReasons);
    }

    /** 分批發送提醒消息（僅家長）。 */
    public boolean sendRemindInBatches(List<String> parentUserIds, String content) {
        if (parentUserIds == null || parentUserIds.isEmpty()) {
            return false;
        }
        int totalBatches = calcBatchCount(parentUserIds.size(), PARENT_BATCH_SIZE);
        for (int i = 0; i < totalBatches; i++) {
            List<String> currentBatch = extractBatch(parentUserIds, i, PARENT_BATCH_SIZE);
            if (currentBatch.isEmpty()) {
                continue;
            }
            try {
                JSONObject result = wechatWorkHttpClient.sendSchoolNotification(
                        buildParentOnlyPayload(currentBatch, content));
                if (isWechatSuccess(result)) {
                    log.info("第 {}/{} 批提醒消息發送成功", i + 1, totalBatches);
                } else {
                    log.error("第 {}/{} 批提醒消息發送失敗: code={}, msg={}",
                            i + 1, totalBatches, result.getInteger("errcode"), result.getString("errmsg"));
                    return false;
                }
            } catch (Exception e) {
                log.error("第 {}/{} 批提醒消息發送異常", i + 1, totalBatches, e);
                return false;
            }
        }
        return true;
    }

    /** 分批發送撤回通知（僅家長）。 */
    public void sendRecallInBatches(List<String> parentUserIds, String content) {
        if (parentUserIds == null || parentUserIds.isEmpty()) {
            return;
        }
        int totalBatches = calcBatchCount(parentUserIds.size(), PARENT_BATCH_SIZE);
        for (int i = 0; i < totalBatches; i++) {
            List<String> batch = extractBatch(parentUserIds, i, PARENT_BATCH_SIZE);
            if (batch.isEmpty()) {
                continue;
            }
            try {
                wechatWorkHttpClient.sendSchoolNotification(buildParentOnlyPayload(batch, content));
            } catch (Exception e) {
                log.error("發送撤回微信通知第 {} 批異常", i + 1, e);
            }
        }
    }

    public JSONObject buildParentOnlyPayload(List<String> parentUserIds, String content) {
        JSONObject payload = new JSONObject();
        payload.put("recv_scope", 0);
        payload.put("to_parent_userid", toJsonArray(parentUserIds));
        payload.put("to_student_userid", new JSONArray());
        payload.put("to_party", new JSONArray());
        payload.put("toall", 0);
        payload.put("msgtype", "text");
        payload.put("agentid", agentId);

        JSONObject text = new JSONObject();
        text.put("content", content);
        payload.put("text", text);

        payload.put("enable_id_trans", 0);
        payload.put("enable_duplicate_check", 0);
        payload.put("duplicate_check_interval", DUPLICATE_CHECK_INTERVAL);
        return payload;
    }

    private JSONObject buildParentOnlyPayload(String parentUserId, String content) {
        return buildParentOnlyPayload(Collections.singletonList(parentUserId), content);
    }

    private void removeInvalidUsers(Set<String> batchSuccessUsers, JSONObject result) {
        String invaliduser = result.getString("invaliduser");
        if (invaliduser != null && !invaliduser.isEmpty()) {
            for (String invalidId : invaliduser.split("\\|")) {
                batchSuccessUsers.remove(invalidId);
            }
        }

        JSONArray invalidParents = result.getJSONArray("invalid_parent_userid");
        if (invalidParents != null) {
            for (int j = 0; j < invalidParents.size(); j++) {
                batchSuccessUsers.remove(invalidParents.getString(j));
            }
        }
    }

    private void markUsersFailed(List<String> userIds, String reason, Map<String, String> failedUserReasons) {
        for (String uid : userIds) {
            failedUserReasons.put(uid, reason);
        }
    }

    private boolean isWechatSuccess(JSONObject result) {
        Integer errcode = result.getInteger("errcode");
        return errcode != null && errcode == 0;
    }

    private String wechatErrorReason(JSONObject result) {
        return "接口返回錯誤: " + result.getInteger("errcode");
    }

    public static int calcBatchCount(int total, int batchSize) {
        return (int) Math.ceil((double) total / batchSize);
    }

    public static List<String> extractBatch(List<String> list, int batchIndex, int batchSize) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        int fromIndex = batchIndex * batchSize;
        if (fromIndex >= list.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(fromIndex + batchSize, list.size());
        return new ArrayList<>(list.subList(fromIndex, toIndex));
    }

    private JSONArray toJsonArray(List<String> values) {
        JSONArray array = new JSONArray();
        if (values != null && !values.isEmpty()) {
            array.addAll(values);
        }
        return array;
    }
}
