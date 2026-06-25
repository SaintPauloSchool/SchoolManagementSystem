package com.sms.handler.notification.support;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.framework.wechat.WechatWorkHttpClient;
import com.sms.system.entity.notification.Notification;
import com.sms.system.entity.notification.SendResult;
import com.sms.system.entity.vo.BatchReceiversVO;
import com.sms.system.entity.vo.BatchSendOutcomeVO;
import com.sms.system.entity.vo.ParentStudentMessageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
 * <p>
 * 封裝企微「發送家校通知」接口的調用方式，支持兩種發送模式：
 * </p>
 * <ul>
 *   <li><b>個性化發送</b>：每位家長按綁定學生/班級生成不同正文，逐條調用接口（{@link #sendWithPersonalization}）</li>
 *   <li><b>批量發送</b>：同一正文，按企微接口上限分批合併發送（{@link #sendInBatches}）</li>
 * </ul>
 * <p>另提供提醒、撤回等場景的批量發送入口。所有請求均通過 {@link #buildSchoolNotificationPayload} 構建 JSON。</p>
 */
@Component
public class NotificationSchoolSendHelper {

    private static final Logger log = LoggerFactory.getLogger(NotificationSchoolSendHelper.class);

    /** 家長/學生 userid 每批上限（企微接口限制 1000） */
    public static final int PARENT_STUDENT_BATCH_SIZE = 1000;
    /** 部門 party id 每批上限（企微接口限制 100） */
    public static final int PARTY_BATCH_SIZE = 100;
    /** 重複消息檢查間隔（秒），寫入 payload 的 duplicate_check_interval */
    private static final int DUPLICATE_CHECK_INTERVAL = 1800;
    /** 個性化發送時並行執行緒池大小 */
    private static final int PERSONALIZED_SEND_POOL_SIZE = 20;

    @Value("${wechat.work.agentId:1000033}")
    private Integer agentId;

    @Autowired
    private WechatWorkHttpClient wechatWorkHttpClient;

    @Autowired
    private NotificationMessageContentHelper messageContentHelper;

    // -------------------------------------------------------------------------
    // 發佈發送：個性化 / 批量
    // -------------------------------------------------------------------------

    /**
     * 按家長-學生綁定逐條發送個性化家校通知。
     * <p>
     * 當 {@code messageInfos} 非空時，按家長分組後並行發送：每條消息正文帶班級名、學生名。
     * 僅使用 {@code to_parent_userid}（單個家長），不走學生/部門通道。
     * </p>
     * <p>若 {@code messageInfos} 為空，降級為 {@link #sendInBatches} 統一正文批量發送。</p>
     *
     * @param parentUserIds  解析後的家長 userid 列表（messageInfos 為空時使用）
     * @param studentUserIds 解析後的學生 userid 列表（當前業務通常為空，保留兼容）
     * @param partyIds       部門 party id 列表
     * @param messageInfos   企微選人產生的家長-學生-班級信息，用於個性化正文
     * @return 成功/失敗計數、成功家長集合、失敗原因映射
     */
    public SendResult sendWithPersonalization(Notification notification,
                                              List<String> parentUserIds,
                                              List<String> studentUserIds,
                                              List<String> partyIds,
                                              List<ParentStudentMessageInfo> messageInfos) {
        if (messageInfos == null || messageInfos.isEmpty()) {
            return sendInBatches(notification, parentUserIds, studentUserIds, partyIds);
        }

        // 按家長分組：同一家長可能對應多個孩子，需分別發送
        Map<String, List<ParentStudentMessageInfo>> parentToMessagesMap = messageInfos.stream()
                .collect(Collectors.groupingBy(ParentStudentMessageInfo::getParentUserId));

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        Set<String> successUserIds = Collections.synchronizedSet(new HashSet<>());
        Map<String, String> failedUserReasons = new ConcurrentHashMap<>();

        // 獨立線程池並行發送，避免阻塞公共 ForkJoinPool
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
                            log.debug("成功發送通知給家長 {}，學生 {}", parentUserId, msgInfo.getStudentUserId());
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

        log.info("通知 {} 已全部發送完成，成功: {}, 失敗: {}",
                notification.getNotificationId(), successCount.get(), failCount.get());
        return new SendResult(successCount.get(), failCount.get(), successUserIds, failedUserReasons);
    }

    /**
     * 使用通知統一正文，按企微接口上限分批發送家校通知。
     * <p>適用於自定義家校（無綁定信息）或無需個性化正文的場景。</p>
     */
    public SendResult sendInBatches(Notification notification,
                                    List<String> parentUserIds,
                                    List<String> studentUserIds,
                                    List<String> partyIds) {
        return sendInBatches(notification.getNotificationId(), parentUserIds, studentUserIds, partyIds,
                nId -> messageContentHelper.buildPublishContent(notification));
    }

    /**
     * 分批發送核心邏輯。
     * <p>
     * 家長、學生、部門三類接收人各自按批次大小切分，取三者批次数的最大值作為總批数；
     * 每批構建一個 payload，調用一次企微接口。
     * </p>
     * <p>
     * 整批接口失敗時，該批所有家長/學生計為失敗；
     * 接口成功但返回無效用戶時，通過 {@link #resolveBatchOutcome} 逐人剔除。
     * </p>
     *
     * @param contentSupplier 按通知 ID 生成該批正文（各批內容相同）
     */
    private SendResult sendInBatches(Long notificationId,
                                     List<String> parentUserIds,
                                     List<String> studentUserIds,
                                     List<String> partyIds,
                                     Function<Long, String> contentSupplier) {
        int parentBatches = calcBatchCount(parentUserIds.size(), PARENT_STUDENT_BATCH_SIZE);
        int studentBatches = calcBatchCount(studentUserIds.size(), PARENT_STUDENT_BATCH_SIZE);
        int partyBatches = calcBatchCount(partyIds.size(), PARTY_BATCH_SIZE);
        int totalBatches = Math.max(Math.max(parentBatches, studentBatches), partyBatches);

        log.info("通知 {} 需要分 {} 批發送（家長 {} 批，學生 {} 批，部門 {} 批）",
                notificationId, totalBatches, parentBatches, studentBatches, partyBatches);

        int successCount = 0;
        int failCount = 0;
        Set<String> successUserIds = new HashSet<>();
        Map<String, String> failedUserReasons = new HashMap<>();

        for (int i = 0; i < totalBatches; i++) {
            BatchReceiversVO batch = getBatchData(parentUserIds, studentUserIds, partyIds, i);
            if (batch.isEmpty()) {
                continue;
            }

            JSONObject payload = buildSchoolNotificationPayload(
                    batch.getParentIds(), batch.getStudentIds(), batch.getPartyIds(),
                    contentSupplier.apply(notificationId));

            log.info("發送通知 {} 的第 {}/{} 批，家長: {}, 學生: {}, 部門: {}",
                    notificationId, i + 1, totalBatches,
                    batch.getParentIds().size(), batch.getStudentIds().size(), batch.getPartyIds().size());

            JSONObject result = wechatWorkHttpClient.sendSchoolNotification(payload);
            if (!isWechatSuccess(result)) {
                // 整批失敗：該批所有家長/學生均記為失敗
                String reason = wechatErrorReason(result);
                log.error("通知 {} 第 {} 批發送失敗: code={}, msg={}",
                        notificationId, i + 1, result.getInteger("errcode"), result.getString("errmsg"));
                failCount += batch.getParentIds().size() + batch.getStudentIds().size();
                markUsersFailed(batch.getParentIds(), reason, failedUserReasons);
                markUsersFailed(batch.getStudentIds(), reason, failedUserReasons);
                continue;
            }

            log.info("通知 {} 第 {}/{} 批發送成功", notificationId, i + 1, totalBatches);
            BatchSendOutcomeVO outcome = resolveBatchOutcome(batch, result);
            if (outcome.getFailCount() > 0) {
                log.warn("通知 {} 第 {} 批有 {} 個無效用戶", notificationId, i + 1, outcome.getFailCount());
            }
            failCount += outcome.getFailCount();
            successCount += outcome.getSuccessUserIds().size();
            successUserIds.addAll(outcome.getSuccessUserIds());
            failedUserReasons.putAll(outcome.getFailedUserReasons());
        }

        log.info("通知 {} 已全部發送完成，共 {} 批，成功: {}, 失敗: {}",
                notificationId, totalBatches, successCount, failCount);
        return new SendResult(successCount, failCount, successUserIds, failedUserReasons);
    }

    // -------------------------------------------------------------------------
    // 提醒 / 撤回
    // -------------------------------------------------------------------------

    /**
     * 分批發送提醒消息（僅家長通道）。
     *
     * @return 全部批次成功返回 true；任一批失敗或異常返回 false
     */
    public boolean sendRemindInBatches(List<String> parentUserIds, String content) {
        if (parentUserIds == null || parentUserIds.isEmpty()) {
            return false;
        }
        int totalBatches = calcBatchCount(parentUserIds.size(), PARENT_STUDENT_BATCH_SIZE);
        for (int i = 0; i < totalBatches; i++) {
            List<String> currentBatch = extractBatch(parentUserIds, i, PARENT_STUDENT_BATCH_SIZE);
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

    /**
     * 分批發送撤回通知（使用原接收對象：家長 + 學生 + 部門）。
     * <p>僅負責調用企微接口，不統計成功/失敗；單批異常記錄日誌後繼續下一批。</p>
     */
    public void sendRecallInBatches(List<String> parentUserIds,
                                    List<String> studentUserIds,
                                    List<String> partyIds,
                                    String content) {
        int totalBatches = Math.max(
                calcBatchCount(parentUserIds.size(), PARENT_STUDENT_BATCH_SIZE),
                Math.max(
                        calcBatchCount(studentUserIds.size(), PARENT_STUDENT_BATCH_SIZE),
                        calcBatchCount(partyIds.size(), PARTY_BATCH_SIZE)));

        for (int i = 0; i < totalBatches; i++) {
            BatchReceiversVO batch = getBatchData(parentUserIds, studentUserIds, partyIds, i);
            if (batch.isEmpty()) {
                continue;
            }
            try {
                JSONObject payload = buildSchoolNotificationPayload(
                        batch.getParentIds(), batch.getStudentIds(), batch.getPartyIds(), content);
                wechatWorkHttpClient.sendSchoolNotification(payload);
            } catch (Exception e) {
                log.error("發送撤回微信通知第 {} 批異常 (原接收對象)", i + 1, e);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Payload 構建
    // -------------------------------------------------------------------------

    /** 僅向家長列表發送，學生與部門列表為空 */
    public JSONObject buildParentOnlyPayload(List<String> parentUserIds, String content) {
        return buildSchoolNotificationPayload(parentUserIds, Collections.emptyList(), Collections.emptyList(), content);
    }

    /** 單個家長的 payload，供個性化逐條發送使用 */
    private JSONObject buildParentOnlyPayload(String parentUserId, String content) {
        return buildParentOnlyPayload(Collections.singletonList(parentUserId), content);
    }

    /**
     * 構建企微「發送家校通知」接口請求體。
     * <p>字段對應企微文檔：recv_scope、to_parent_userid、to_student_userid、to_party、text 等。</p>
     */
    JSONObject buildSchoolNotificationPayload(List<String> parentUserIds,
                                              List<String> studentUserIds,
                                              List<String> partyIds,
                                              String content) {
        JSONObject payload = new JSONObject();
        payload.put("recv_scope", 0);
        payload.put("to_parent_userid", toJsonArray(parentUserIds));
        payload.put("to_student_userid", toJsonArray(studentUserIds));
        payload.put("to_party", toJsonArray(partyIds));
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

    // -------------------------------------------------------------------------
    // 批量發送結果解析
    // -------------------------------------------------------------------------

    /**
     * 根據企微返回的無效用戶列表，從本批「假定成功」集合中剔除，得到實際成功/失敗人數。
     * <p>僅統計家長與學生 userid，部門發送不計入 successUserIds。</p>
     */
    private BatchSendOutcomeVO resolveBatchOutcome(BatchReceiversVO batch, JSONObject result) {
        Set<String> batchSuccessUsers = new HashSet<>(batch.getParentIds().size() + batch.getStudentIds().size());
        batchSuccessUsers.addAll(batch.getParentIds());
        batchSuccessUsers.addAll(batch.getStudentIds());
        removeInvalidUsers(batchSuccessUsers, result);

        Set<String> batchFailedUsers = new HashSet<>(batch.getParentIds());
        batchFailedUsers.addAll(batch.getStudentIds());
        batchFailedUsers.removeAll(batchSuccessUsers);

        Map<String, String> failedUserReasons = new HashMap<>();
        for (String failedId : batchFailedUsers) {
            failedUserReasons.put(failedId, "無效用戶或微信端未關注");
        }

        int batchTotal = batch.getParentIds().size() + batch.getStudentIds().size();
        return new BatchSendOutcomeVO(batchSuccessUsers, batchTotal - batchSuccessUsers.size(), failedUserReasons);
    }

    /**
     * 從成功集合中移除企微返回的無效用戶。
     * <p>兼容三種返回格式：invaliduser（| 分隔）、invalid_parent_userid、invalid_student_userid。</p>
     */
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

        JSONArray invalidStudents = result.getJSONArray("invalid_student_userid");
        if (invalidStudents != null) {
            for (int j = 0; j < invalidStudents.size(); j++) {
                batchSuccessUsers.remove(invalidStudents.getString(j));
            }
        }
    }

    /** 將一批用戶全部標記為同一失敗原因（整批接口失敗時使用） */
    private void markUsersFailed(List<String> userIds, String reason, Map<String, String> failedUserReasons) {
        for (String uid : userIds) {
            failedUserReasons.put(uid, reason);
        }
    }

    /** 判斷企微接口響應是否成功（errcode == 0） */
    private boolean isWechatSuccess(JSONObject result) {
        Integer errcode = result.getInteger("errcode");
        return errcode != null && errcode == 0;
    }

    /** 從企微響應提取簡要錯誤描述 */
    private String wechatErrorReason(JSONObject result) {
        return "接口返回錯誤: " + result.getInteger("errcode");
    }

    // -------------------------------------------------------------------------
    // 分批工具方法
    // -------------------------------------------------------------------------

    /** 計算列表按指定批次大小需要分幾批（向上取整） */
    public static int calcBatchCount(int total, int batchSize) {
        return (int) Math.ceil((double) total / batchSize);
    }

    /** 截取第 batchIndex 批的元素子列表（越界返回空列表） */
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

    /**
     * 按批次索引同時切分家長、學生、部門三類接收人。
     * <p>家長/學生使用 {@link #PARENT_STUDENT_BATCH_SIZE}，部門使用 {@link #PARTY_BATCH_SIZE}。</p>
     */
    static BatchReceiversVO getBatchData(List<String> parentUserIds,
                                         List<String> studentUserIds,
                                         List<String> partyIds,
                                         int batchIndex) {
        return new BatchReceiversVO(
                extractBatch(parentUserIds, batchIndex, PARENT_STUDENT_BATCH_SIZE),
                extractBatch(studentUserIds, batchIndex, PARENT_STUDENT_BATCH_SIZE),
                extractBatch(partyIds, batchIndex, PARTY_BATCH_SIZE)
        );
    }

    /** 將字符串列表轉為 Fastjson JSONArray，null/空列表返回空數組 */
    private JSONArray toJsonArray(List<String> values) {
        JSONArray array = new JSONArray();
        if (values != null && !values.isEmpty()) {
            array.addAll(values);
        }
        return array;
    }

}
