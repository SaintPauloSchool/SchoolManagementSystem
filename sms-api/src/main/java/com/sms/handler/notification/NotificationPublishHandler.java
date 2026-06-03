package com.sms.handler.notification;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.common.utils.security.Md5Utils;
import com.sms.framework.wechat.WechatWorkHttpClient;
import com.sms.system.entity.task.TaskResult;
import com.sms.system.entity.notification.*;
import com.sms.system.entity.vo.ResolvedReceiversVO;
import com.sms.system.entity.vo.ParentStudentMessageInfo;
import com.sms.system.entity.vo.UnrepliedStudentVO;
import com.sms.system.entity.vo.BatchReceiversVO;
import com.sms.system.service.notification.*;
import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.service.INotificationMessageService;
import com.sms.system.service.ISysDepartmentParentBindingService;
import com.sms.system.mapper.SysAdminMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 通告發佈處理器
 * 負責將通告消息推送給指定的接收者（如通過企業微信發送家校通知）
 */
@Component
public class NotificationPublishHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublishHandler.class);

    // 每批最多發送的家長/學生數量
    private static final int PARENT_STUDENT_BATCH_SIZE = 1000;
    // 每批最多發送的部門數量
    private static final int PARTY_BATCH_SIZE = 100;
    // 防重複發送校驗時間（秒）
    private static final int DUPLICATE_CHECK_INTERVAL = 1800;
    // 發送狀態常量
    private static final String SEND_STATUS_SUCCESS = "2";
    private static final String SEND_STATUS_FAIL = "3";
    private static final String SEND_STATUS_PARTIAL = "4";
    // 線程安全的日期格式化器
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 默認的通告查看基礎 URL。如果通告本身沒有跳轉鏈接，將使用此基礎 URL 拼接通告 ID。
     */
    @Value("${wechat.work.noticeBaseUrl:http://10.32.96.55:8080/notice/}")
    private String noticeBaseUrl;

    /**
     * 抄送通知的查看基礎 URL。如果抄送通知本身沒有跳轉鏈接，將使用此基礎 URL 拼接通告 ID。
     */
    @Value("${wechat.work.ccNoticeBaseUrl:http://10.32.96.55:8080/cc-notice/}")
    private String ccNoticeBaseUrl;

    /**
     * 學生手冊的查看基礎 URL。
     */
    @Value("${wechat.work.handbookBaseUrl:http://10.32.96.55:8080/handbook}")
    private String handbookBaseUrl;

    @Value("${wechat.work.corpId:ww04fad852e91fd490}")
    private String corpId;

    @Value("${wechat.work.agentId:1000033}")
    private Integer agentId;

    @Value("${sms.encryption.salt}")
    private String encryptionSalt;

    @Autowired
    private INotificationReceiverService notificationReceiverService;

    @Autowired
    private INotificationCcService notificationCcService;

    @Autowired
    private WechatWorkHttpClient wechatWorkHttpClient;

    @Autowired
    private INotificationSendRecordService notificationSendRecordService;

    @Autowired
    private INotificationUserReadRecordService notificationUserReadRecordService;

    @Autowired
    private INotificationReminderRecordService notificationReminderRecordService;

    @Autowired
    private INotificationService notificationService;

    @Autowired
    private INotificationResendFailRecordService notificationResendFailRecordService;

    @Autowired
    private INotificationMessageService notificationMessageService;

    @Autowired
    private ISysDepartmentParentBindingService departmentParentBindingService;

    @Autowired
    private SysAdminMapper sysAdminMapper;

    // =========================================================================
    // A. Public 入口方法 (Public API)
    // =========================================================================

    /**
     * 將通告發佈到微信（家校通信/外部聯繫人消息）
     *
     * @param notification 通告實體對象
     * @param receivers    通告的接收者列表設置
     * @throws IllegalStateException 當沒有解析出有效的接收者，或微信發送失敗時拋出異常
     */
    public void publishToWechat(Notification notification, List<NotificationReceiver> receivers) {
        // 1. 解析接收者，提取家長、學生、部門的 ID 列表以及精確的綁定關係
        ResolvedReceiversVO resolvedReceivers = notificationReceiverService.resolveReceivers(receivers);

        List<String> parentUserIds = nullSafe(resolvedReceivers.getParentUserIds());
        List<String> studentUserIds = nullSafe(resolvedReceivers.getStudentUserIds());
        List<String> partyIds = nullSafe(resolvedReceivers.getPartyIds());
        List<SysDepartmentParentBinding> bindings = nullSafe(resolvedReceivers.getBindings());

        // 如果沒有任何接收者，則拋出異常避免無效調用
        if (parentUserIds.isEmpty() && studentUserIds.isEmpty() && partyIds.isEmpty()) {
            log.warn("未找到有效的微信收件人進行通知 {}", notification.getNotificationId());
            throw new IllegalStateException("未解析出有效的微信接收者");
        }

        // 2. 使用 Service 批量構建消息信息（包含班級名 and 學生名）
        List<ParentStudentMessageInfo> messageInfos = notificationMessageService.buildMessageInfos(bindings);

        // 3. 分批發送通知，並獲取發送結果
        SendResult sendResult = sendInBatchesWithPersonalization(notification, parentUserIds, studentUserIds, partyIds,
                messageInfos);

        // 4. 創建發送記錄（包含成功/失敗統計）
        NotificationSendRecord sendRecord = createSendRecord(notification, studentUserIds, sendResult, bindings);
        notificationSendRecordService.save(sendRecord);

        // 5. 創建用戶閱讀記錄（帶入每個用戶的發送成功狀態）
        List<NotificationUserReadRecord> readRecords = createUserReadRecords(
                sendRecord.getSendRecordId(), parentUserIds, studentUserIds, sendResult.getSuccessUserIds(), bindings);
        notificationUserReadRecordService.batchSave(readRecords);
    }

    /**
     * 發送抄送通知到企業微信應用消息
     *
     * @param notification 通告實體對象
     */
    public void sendCcNotifications(Notification notification) {
        // 獲取抄送用戶 ID 集合
        Set<String> allUserIds = getCcUserIds(notification.getNotificationId());

        // 如果沒有任何接收者，直接返回
        if (allUserIds.isEmpty()) {
            log.info("通知 {} 沒有設置抄送對象且無管理員配置", notification.getNotificationId());
            return;
        }

        // 分批發送抄送消息
        sendCcInBatches(notification, new ArrayList<>(allUserIds));
    }

    /**
     * 發送撤回通知給原接收對象 and 抄送對象
     *
     * @param originalNotification 原通知實體對象
     */
    public void sendRecallNotification(Notification originalNotification) {
        if (originalNotification == null || originalNotification.getNotificationId() == null) {
            return;
        }

        // 1. 查詢並解析原通知的接收者
        List<NotificationReceiver> receivers = notificationReceiverService.selectByNotificationId(originalNotification.getNotificationId());
        ResolvedReceiversVO resolvedReceivers = notificationReceiverService.resolveReceivers(receivers);

        List<String> parentUserIds = nullSafe(resolvedReceivers.getParentUserIds());
        List<String> studentUserIds = nullSafe(resolvedReceivers.getStudentUserIds());
        List<String> partyIds = nullSafe(resolvedReceivers.getPartyIds());

        // 2. 構建撤回內容
        String title = originalNotification.getTitle() == null ? "" : originalNotification.getTitle().trim();
        String recallTime = LocalDateTime.now().format(DATE_FORMATTER);
        String recallContent = "📢 您有一條通告被撤回\n"
                + "──────────────\n"
                + "📌 標題：\n" + title + "\n\n"
                + "🕒 撤回時間：\n" + recallTime;

        // 3. 分批發送撤回通知給原接收對象（家校通知接口）
        if (!parentUserIds.isEmpty() || !studentUserIds.isEmpty() || !partyIds.isEmpty()) {
            sendRecallToReceiversInBatches(parentUserIds, studentUserIds, partyIds, recallContent);
        }

        // 4. 獲取抄送對象與管理員用戶 ID 集合
        Set<String> ccUserIds = getCcUserIds(originalNotification.getNotificationId());

        // 5. 分批發送撤回通知給抄送對象與管理員（應用消息接口 - 純文本）
        if (!ccUserIds.isEmpty()) {
            sendRecallToCcInBatches(new ArrayList<>(ccUserIds), recallContent);
        }
    }

    /**
     * 提示家長回復（重新發送通知給未回復的學生家長）- 通過通知 ID
     *
     * @param notificationId 通知ID
     * @return 發送結果統計
     */
    public Map<String, Object> remindParentsToReply(Long notificationId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 查詢原始通知
        Notification notification = notificationService.selectNotificationById(notificationId);
        if (notification == null) {
            throw new IllegalStateException("未找到通知信息");
        }

        // 2. 檢查是否超過回復截止時間
        if (notification.getReplyDeadline() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(notification.getReplyDeadline())) {
                result.put("success", false);
                result.put("message", "已超過回復截止時間，無法提示家長回復");
                result.put("remindCount", 0);
                return result;
            }
        }

        return remindParentsToReply(notification);
    }

    /**
     * 提示家長回復（根據通知實體）
     *
     * @param notification 通知實體
     * @return 發送結果統計
     */
    public Map<String, Object> remindParentsToReply(Notification notification) {
        // 發送結果統計
        Map<String, Object> result = new HashMap<>();
        // 1. 獲取通知ID
        Long notificationId = notification.getNotificationId();

        // 3. 查詢發送記錄
        NotificationSendRecord sendRecord = notificationSendRecordService.selectByNotificationId(notificationId);
        if (sendRecord == null) {
            throw new IllegalStateException("未找到發送記錄");
        }

        // 4. 查詢未回復的學生列表（按學生分組，只要有一個家長回復就算已回復）
        List<UnrepliedStudentVO> unrepliedStudents = notificationUserReadRecordService
                .selectUnrepliedStudents(sendRecord.getSendRecordId());

        // 5. 如果沒有未回復的學生，則返回成功
        if (unrepliedStudents == null || unrepliedStudents.isEmpty()) {
            result.put("success", true);
            result.put("message", "所有 student 家長均已回復");
            result.put("remindCount", 0);
            return result;
        }

        log.info("開始發送提醒通知，共 {} 個學生未回復", unrepliedStudents.size());

        // 創建提醒記錄列表
        int successCount = 0;
        int failCount = 0;
        List<NotificationReminderRecord> reminderRecords = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 構建提醒消息內容（只需要構建一次）
        String remindContent = buildRemindContent(notification);

        // 6. 爲每個未回復的學生發送提醒通知
        for (UnrepliedStudentVO student : unrepliedStudents) {
            String studentUserId = student.getStudentUserId();
            List<String> parentUserIdList = student.getParentUserIds();

            if (studentUserId == null || parentUserIdList == null || parentUserIdList.isEmpty()) {
                continue;
            }

            // 直接使用 List 的 toString() 方法存儲爲字符串
            String parentUserIdsStr = parentUserIdList.toString();

            try {
                // 分批發送提醒消息
                boolean sendSuccess = sendRemindInBatches(parentUserIdList, remindContent);

                // 建立提醒記錄
                reminderRecords.add(buildReminderRecord(
                        notificationId, sendRecord.getSendRecordId(),
                        studentUserId, parentUserIdsStr, now, sendSuccess ? "1" : "2"));

                if (sendSuccess) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                log.error("發送提醒通知失敗，學生ID: {}", studentUserId, e);
                failCount++;

                // 即使失敗也建立記錄
                reminderRecords.add(buildReminderRecord(
                        notificationId, sendRecord.getSendRecordId(),
                        studentUserId, parentUserIdsStr, now, "2"));
            }
        }

        // 7. 批量保存提醒記錄
        if (!reminderRecords.isEmpty()) {
            notificationReminderRecordService.batchSave(reminderRecords);
        }

        // 8. 構建返回結果
        result.put("success", true);
        result.put("remindCount", unrepliedStudents.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);

        // 根據發送結果生成不同的提示信息
        if (failCount == 0) {
            // 全部成功
            result.put("message", String.format("提醒發送成功，共發送 %d 個學生", successCount));
        } else if (successCount == 0) {
            // 全部失敗
            result.put("success", false);
            result.put("message", String.format("微信發送失敗，共 %d 個學生未能發送提醒", failCount));
        } else {
            // 部分成功
            result.put("message", String.format("提醒發送完成，成功 %d 個，失敗 %d 個（微信發送異常）", successCount, failCount));
        }

        return result;
    }

    /**
     * 重新發送失敗通知（根據通知ID找到發送失敗的用戶重新發送）
     *
     * @param notificationId 通知ID
     * @param isAutoTask     是否是定時任務自動重發（自動重發會記錄失敗次數，滿3次不再重發）
     * @return 發送結果統計
     */
    public Map<String, Object> resendFailedNotifications(Long notificationId, boolean isAutoTask) {
        Map<String, Object> result = new HashMap<>();

        // 1. 查詢原始通知
        Notification notification = notificationService.selectNotificationById(notificationId);
        if (notification == null) {
            throw new IllegalStateException("未找到通知信息");
        }

        // 2. 查詢發送記錄
        NotificationSendRecord sendRecord = notificationSendRecordService.selectByNotificationId(notificationId);
        if (sendRecord == null) {
            throw new IllegalStateException("未找到發送記錄");
        }

        // 3. 查詢發送失敗的閱讀記錄
        List<NotificationUserReadRecord> failedRecords = notificationUserReadRecordService
                .selectFailedRecords(sendRecord.getSendRecordId());

        // 4. 如果是自動任務，過濾掉已經達到最大失敗次數（放棄重發）的用戶
        if (isAutoTask && failedRecords != null) {
            Set<String> abandonedIds = notificationResendFailRecordService.selectAbandonedUserIds(notificationId);
            failedRecords = failedRecords.stream()
                    .filter(record -> !abandonedIds.contains(record.getUserId()))
                    .collect(Collectors.toList());
        }

        // 沒有失敗的記錄則結束
        if (failedRecords == null || failedRecords.isEmpty()) {
            result.put("success", true);
            result.put("message", "沒有需要重發的失敗記錄");
            result.put("resendCount", 0);
            return result;
        }

        log.info("開始重新發送失敗通知，共 {} 條失敗記錄", failedRecords.size());

        // 5. 按用戶類型分組失敗記錄
        List<String> failedParentIds = new ArrayList<>();
        List<String> failedStudentIds = new ArrayList<>();
        for (NotificationUserReadRecord record : failedRecords) {
            if ("2".equals(record.getUserType())) {
                failedParentIds.add(record.getUserId());
            } else if ("1".equals(record.getUserType())) {
                failedStudentIds.add(record.getUserId());
            }
        }

        // 6. 重新發送並更新每條閱讀記錄 of send_status
        Set<String> overallSuccessUserIds = new HashSet<>();
        // 保存所有失敗用戶的失敗原因
        Map<String, String> allFailedUserReasons = new HashMap<>();

        // 重新發送家長消息
        if (!failedParentIds.isEmpty()) {
            SendResult parentResult = sendInBatches(notification, failedParentIds, Collections.emptyList(),
                    Collections.emptyList());
            updateReadRecords(failedRecords, "2", parentResult.getSuccessUserIds());
            overallSuccessUserIds.addAll(parentResult.getSuccessUserIds());
            if (parentResult.getFailedUserReasons() != null)
                allFailedUserReasons.putAll(parentResult.getFailedUserReasons());
        }

        // 重新發送學生消息
        if (!failedStudentIds.isEmpty()) {
            SendResult studentResult = sendInBatches(notification, Collections.emptyList(), failedStudentIds,
                    Collections.emptyList());
            updateReadRecords(failedRecords, "1", studentResult.getSuccessUserIds());
            overallSuccessUserIds.addAll(studentResult.getSuccessUserIds());
            if (studentResult.getFailedUserReasons() != null)
                allFailedUserReasons.putAll(studentResult.getFailedUserReasons());
        }

        // 如果是自動重發， 記錄自動重發的失敗信息
        if (isAutoTask) {
            for (NotificationUserReadRecord record : failedRecords) {
                if (!overallSuccessUserIds.contains(record.getUserId())) {
                    NotificationResendFailRecord failRecord = new NotificationResendFailRecord();
                    failRecord.setNotificationId(notificationId);
                    failRecord.setSendRecordId(sendRecord.getSendRecordId());
                    failRecord.setUserId(record.getUserId());
                    failRecord.setUserType(record.getUserType());
                    failRecord.setStudentUserId(record.getStudentUserId());
                    String reason = allFailedUserReasons.getOrDefault(record.getUserId(), "未知原因");
                    failRecord.setFailReason1("自動重發失敗");
                    failRecord.setFailMessage1(reason);
                    notificationResendFailRecordService.saveOrUpdate(failRecord);
                }
            }
        }

        // 7. 以學生爲維度統計成功/失敗數（與 createSendRecord 邏輯一致）
        // 建立 studentUserId -> 該學生下所有 userId 集合
        Map<String, Set<String>> studentToUsersMap = new HashMap<>();
        for (NotificationUserReadRecord record : failedRecords) {
            String studentId = record.getStudentUserId();
            if (studentId == null || studentId.trim().isEmpty()) {
                studentId = record.getUserId();
            }
            studentToUsersMap.computeIfAbsent(studentId, k -> new HashSet<>()).add(record.getUserId());
        }

        int successCount = 0;
        int failCount = 0;
        for (Map.Entry<String, Set<String>> entry : studentToUsersMap.entrySet()) {
            boolean anySuccess = false;
            for (String uid : entry.getValue()) {
                if (overallSuccessUserIds.contains(uid)) {
                    anySuccess = true;
                    break;
                }
            }
            if (anySuccess)
                successCount++;
            else
                failCount++;
        }

        // 8. 更新發送記錄的統計信息（以學生爲維度）
        NotificationSendRecord updateRecord = buildSendRecordUpdate(sendRecord, successCount);
        notificationSendRecordService.update(updateRecord);

        // 9. 構建返回結果
        result.put("resendCount", studentToUsersMap.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);

        // 10. 構建返回結果文字
        if (failCount == 0) {
            result.put("success", true);
            result.put("message", String.format("重發成功，共 %d 個學生", successCount));
        } else if (successCount == 0) {
            result.put("success", false);
            result.put("message", String.format("重發失敗，共 %d 個學生未能發送", failCount));
        } else {
            result.put("success", true);
            result.put("message", String.format("重發完成，成功 %d 個學生，失敗 %d 個學生", successCount, failCount));
        }

        return result;
    }

    /**
     * 每日學校通知發送主方法
     * 由 SchoolNoticeTask 調用（每周一至周五下午 6 點）
     */
    public TaskResult sendDailySchoolNotice() {
        log.info("開始執行學校通知發送任務");
        // 獲取所有家長用戶 ID
        List<String> allParentUserIds = departmentParentBindingService.getAllParentUserIds();
        if (allParentUserIds == null || allParentUserIds.isEmpty()) {
            log.warn("沒有家長用戶 ID，跳過發送");
            return TaskResult.success(0, 0, "無家長需發送");
        }
        log.info("獲取到家長用戶 ID 總數量: {}", allParentUserIds.size());

        int totalBatches = calcBatchCount(allParentUserIds.size(), PARENT_STUDENT_BATCH_SIZE);
        log.info("需要分 {} 批發送，每批最多 {} 個家長", totalBatches, PARENT_STUDENT_BATCH_SIZE);

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMsg = new StringBuilder();

        for (int i = 0; i < allParentUserIds.size(); i += PARENT_STUDENT_BATCH_SIZE) {
            int endIndex = Math.min(i + PARENT_STUDENT_BATCH_SIZE, allParentUserIds.size());
            List<String> batchList = allParentUserIds.subList(i, endIndex);
            int batchNumber = (i / PARENT_STUDENT_BATCH_SIZE) + 1;

            log.info("開始發送第 {}/{} 批，本批家長數量: {}", batchNumber, totalBatches, batchList.size());

            String content = "📚 今日學生手冊\n\n"
                    + "🔗 請點擊以下連接查看今日學生手冊：\n"
                    + handbookBaseUrl;

            JSONObject result = wechatWorkHttpClient.sendSchoolNotification(
                    buildParentOnlyPayload(batchList, content));

            if (result != null && result.getInteger("errcode") != null && result.getInteger("errcode") == 0) {
                successCount++;
                log.info("第 {}/{} 批發送成功", batchNumber, totalBatches);
            } else {
                failCount++;
                String errmsg = result != null ? result.getString("errmsg") : "null";
                if (errorMsg.length() < 1000) {
                    errorMsg.append("批次 ").append(batchNumber).append(" 失敗: ").append(errmsg).append("; ");
                }
                log.error("第 {}/{} 批發送失敗: errmsg={}", batchNumber, totalBatches, errmsg);
            }
        }

        if (failCount > 0) {
            return new TaskResult(successCount, failCount, "學校通知發送部分失敗: " + errorMsg.toString());
        }

        log.info("學校通知發送完成 - 總批次: {}, 成功: {}, 失敗: {}", totalBatches, successCount, failCount);
        return TaskResult.success(successCount, 0, "全部發送成功");
    }

    /**
     * 批量提醒家長回復通知主方法
     * 由 NotificationReminderTask 調用（每天 9 點 30 分）
     */
    public TaskResult remindAllPendingNotifications() {
        log.info("開始執行定時提示家長回復通知任務");

        Notification queryParam = new Notification();
        queryParam.setStatus("1"); // 1-已發布
        queryParam.setReminderTime(LocalDateTime.now());
        List<Notification> notificationList = notificationService.selectNotificationList(queryParam);

        if (notificationList == null || notificationList.isEmpty()) {
            log.info("今日無需提醒回復的通知，任務結束");
            return TaskResult.success(0, 0, "無通知需提醒");
        }

        int remindCount = 0;
        int failCount = 0;
        StringBuilder errorMsg = new StringBuilder();

        for (Notification notification : notificationList) {
            log.info("發現需要提示回復的通知: 標題={}, ID={}",
                    notification.getTitle(), notification.getNotificationId());
            try {
                Map<String, Object> result = remindParentsToReply(notification);
                if (result != null && Boolean.FALSE.equals(result.get("success"))) {
                    failCount++;
                    if (errorMsg.length() < 1000) {
                        errorMsg.append("通知ID ").append(notification.getNotificationId()).append(" 失敗: ")
                                .append(result.get("message")).append("; ");
                    }
                } else {
                    remindCount++;
                }
            } catch (Exception e) {
                failCount++;
                if (errorMsg.length() < 1000) {
                    errorMsg.append("通知ID ").append(notification.getNotificationId()).append(" 失敗: ")
                            .append(e.getMessage()).append("; ");
                }
                log.error("定時提示回復失敗: notificationId={}", notification.getNotificationId(), e);
            }
        }

        if (failCount > 0) {
            return new TaskResult(remindCount, failCount, "定時提示家長回復通知部分失敗: " + errorMsg.toString());
        }

        log.info("定時提示家長回復通知任務執行完成，共處理 {} 個通知", remindCount);
        return TaskResult.success(remindCount, 0, "全部處理成功");
    }

    /**
     * 批量重發失敗通知主方法
     * 由 NotificationResendTask 調用（每天 9 點至 18 點每小時執行）
     */
    public TaskResult resendAllFailedNotifications() {
        log.info("開始執行定時重新發送失敗通知任務");

        // 獲取所有發送失敗的通知記錄
        List<NotificationSendRecord> failedSendRecords = notificationSendRecordService.selectAllFailedRecords();

        if (failedSendRecords == null || failedSendRecords.isEmpty()) {
            log.info("沒有發送失敗的通知記錄，任務結束");
            return TaskResult.success(0, 0, "無失敗通知需重發");
        }

        log.info("共有 {} 條發送失敗的通知記錄，開始逐一重發", failedSendRecords.size());

        int successNotifications = 0;
        int failNotifications = 0;

        for (NotificationSendRecord sendRecord : failedSendRecords) {
            Long notificationId = sendRecord.getNotificationId();
            try {
                log.info("重發失敗通知: notificationId={}", notificationId);
                Map<String, Object> result = resendFailedNotifications(notificationId, true);
                if (result != null && (Boolean.FALSE.equals(result.get("success"))
                        || (Integer) result.getOrDefault("failCount", 0) > 0)) {
                    failNotifications++;
                    log.error("重發失敗通知異常: notificationId={}, {}", notificationId, result.get("message"));
                } else {
                    successNotifications++;
                }
            } catch (Exception e) {
                failNotifications++;
                log.error("重發失敗通知異常: notificationId={}", notificationId, e);
            }
        }

        if (failNotifications > 0) {
            return new TaskResult(successNotifications, failNotifications, "共 " + failNotifications + " 筆通知重發失敗");
        }

        log.info("定時重新發送失敗通知任務執行完成，成功處理 {} 個通知，失敗 {} 個通知",
                successNotifications, failNotifications);
        return TaskResult.success(successNotifications, 0, "全部處理成功");
    }

    // =========================================================================
    // B. Private 核心發送與構建細節 (Private Core Flow Helpers)
    // =========================================================================

    /**
     * 帶個性化消息的分批發送通知
     */
    private SendResult sendInBatchesWithPersonalization(Notification notification, List<String> parentUserIds,
                                                        List<String> studentUserIds, List<String> partyIds,
                                                        List<ParentStudentMessageInfo> messageInfos) {
        if (messageInfos == null || messageInfos.isEmpty()) {
            return sendInBatches(notification, parentUserIds, studentUserIds, partyIds);
        }

        // 按家長用戶 ID 分組消息
        Map<String, List<ParentStudentMessageInfo>> parentToMessagesMap = messageInfos.stream()
                .collect(Collectors.groupingBy(ParentStudentMessageInfo::getParentUserId));

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        Set<String> successUserIds = Collections.synchronizedSet(new HashSet<>());
        Map<String, String> failedUserReasons = new ConcurrentHashMap<>();

        // 使用專屬的自定義 ForkJoinPool (20條線程) 進行平行發送
        ForkJoinPool customThreadPool = new ForkJoinPool(20);

        try {
            customThreadPool.submit(() -> {
                parentToMessagesMap.entrySet().parallelStream().forEach(entry -> {
                    String parentUserId = entry.getKey();
                    List<ParentStudentMessageInfo> parentMessages = entry.getValue();

                    for (ParentStudentMessageInfo msgInfo : parentMessages) {
                        try {
                            String content = buildContent(notification, msgInfo.getClassName(),
                                    msgInfo.getStudentName(), msgInfo.getStudentUserId());

                            JSONObject payload = buildPersonalizedPayload(parentUserId, content);

                            JSONObject result = wechatWorkHttpClient.sendSchoolNotification(payload);
                            Integer errcode = result.getInteger("errcode");

                            if (errcode != null && errcode == 0) {
                                successCount.incrementAndGet();
                                successUserIds.add(parentUserId);
                                log.debug("成功發送通知給家長 {}，學生 {}", parentUserId, msgInfo.getStudentUserId());
                            } else {
                                failCount.incrementAndGet();
                                String reason = "接口返回錯誤: " + errcode;
                                failedUserReasons.put(parentUserId, reason);
                                log.error("發送通知給家長 {} 失敗: code={}, msg={}", parentUserId, errcode,
                                        result.getString("errmsg"));
                            }
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                            failedUserReasons.put(parentUserId, "發送異常: " + e.getMessage());
                            log.error("發送通知給家長 {} 異常", parentUserId, e);
                        }
                    }
                });
            }).get();
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
     * 分批發送通知，根據企業微信 API 的人數限制進行分批
     */
    private SendResult sendInBatches(Notification notification, List<String> parentUserIds,
                                     List<String> studentUserIds, List<String> partyIds) {
        int parentBatches = calcBatchCount(parentUserIds.size(), PARENT_STUDENT_BATCH_SIZE);
        int studentBatches = calcBatchCount(studentUserIds.size(), PARENT_STUDENT_BATCH_SIZE);
        int partyBatches = calcBatchCount(partyIds.size(), PARTY_BATCH_SIZE);
        int totalBatches = Math.max(Math.max(parentBatches, studentBatches), partyBatches);

        log.info("通知 {} 需要分 {} 批發送（家長 {} 批，學生 {} 批，部門 {} 批）",
                notification.getNotificationId(), totalBatches, parentBatches, studentBatches, partyBatches);

        int successCount = 0;
        int failCount = 0;
        Set<String> successUserIds = new HashSet<>();
        Map<String, String> failedUserReasons = new HashMap<>();

        for (int i = 0; i < totalBatches; i++) {
            BatchReceiversVO batch = getBatchData(parentUserIds, studentUserIds, partyIds, i);

            if (batch.isEmpty()) {
                continue;
            }

            JSONObject payload = buildWechatPayload(batch.getParentIds(), batch.getStudentIds(), batch.getPartyIds(), notification);

            log.info("發送通知 {} 的第 {}/{} 批，家長: {}, 學生: {}, 部門: {}",
                    notification.getNotificationId(), i + 1, totalBatches,
                    batch.getParentIds().size(), batch.getStudentIds().size(), batch.getPartyIds().size());

            JSONObject result = wechatWorkHttpClient.sendSchoolNotification(payload);

            Integer errcode = result.getInteger("errcode");
            if (errcode == null || errcode != 0) {
                String errmsg = result.getString("errmsg");
                log.error("通知 {} 第 {} 批發送失敗: code={}, msg={}",
                        notification.getNotificationId(), i + 1, errcode, errmsg);
                failCount += batch.getParentIds().size() + batch.getStudentIds().size();

                String reason = "接口返回錯誤: " + errcode;
                for (String uid : batch.getParentIds())
                    failedUserReasons.put(uid, reason);
                for (String uid : batch.getStudentIds())
                    failedUserReasons.put(uid, reason);
            } else {
                log.info("通知 {} 第 {}/{} 批發送成功", notification.getNotificationId(), i + 1, totalBatches);

                Set<String> batchSuccessUsers = new HashSet<>(batch.getParentIds().size() + batch.getStudentIds().size());
                batchSuccessUsers.addAll(batch.getParentIds());
                batchSuccessUsers.addAll(batch.getStudentIds());

                String invaliduser = result.getString("invaliduser");
                if (invaliduser != null && !invaliduser.isEmpty()) {
                    String[] invalidUsers = invaliduser.split("\\|");
                    for (String invalidId : invalidUsers) {
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

                Set<String> batchFailedUsers = new HashSet<>(batch.getParentIds());
                batchFailedUsers.addAll(batch.getStudentIds());
                batchFailedUsers.removeAll(batchSuccessUsers);
                for (String failedId : batchFailedUsers) {
                    failedUserReasons.put(failedId, "無效用戶或微信端未關注");
                }

                int batchTotal = batch.getParentIds().size() + batch.getStudentIds().size();
                int batchFailCount = batchTotal - batchSuccessUsers.size();

                if (batchFailCount > 0) {
                    log.warn("通知 {} 第 {} 批有 {} 個無效用戶", notification.getNotificationId(), i + 1, batchFailCount);
                }

                failCount += batchFailCount;
                successCount += batchSuccessUsers.size();
                successUserIds.addAll(batchSuccessUsers);
            }
        }

        log.info("通知 {} 已全部發送完成，共 {} 批，成功: {}, 失敗: {}",
                notification.getNotificationId(), totalBatches, successCount, failCount);

        return new SendResult(successCount, failCount, successUserIds, failedUserReasons);
    }

    /**
     * 構建發送給企業微信家校通知接口的通用 JSON 數據實體
     */
    private JSONObject buildSchoolNotificationPayload(List<String> parentUserIds, List<String> studentUserIds,
                                                      List<String> partyIds, String content) {
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

    /**
     * 構建發送給企業微信接口的 JSON 數據實體
     */
    private JSONObject buildWechatPayload(List<String> parentUserIds, List<String> studentUserIds,
                                          List<String> partyIds, Notification notification) {
        return buildSchoolNotificationPayload(parentUserIds, studentUserIds, partyIds, buildContent(notification));
    }

    /**
     * 構建立只發送給家長的 payload（支持批量）
     */
    private JSONObject buildParentOnlyPayload(List<String> parentUserIds, String content) {
        return buildSchoolNotificationPayload(parentUserIds, Collections.emptyList(), Collections.emptyList(), content);
    }

    /**
     * 構建個性化消息的發送 payload（單個家長）
     */
    private JSONObject buildPersonalizedPayload(String parentUserId, String content) {
        return buildParentOnlyPayload(Collections.singletonList(parentUserId), content);
    }

    /**
     * 構建消息文本內容
     */
    private String buildContent(Notification notification) {
        return buildContent(notification, null, null, null);
    }

    /**
     * 構建個性化消息文本內容（帶班級名和學生名）
     */
    private String buildContent(Notification notification, String className, String studentName, String studentUserId) {
        String title = notification.getTitle() == null ? "" : notification.getTitle().trim();
        String noticeUrl;

        if (studentUserId != null && !studentUserId.trim().isEmpty()) {
            String encryptedStudentId = Md5Utils.encryptSensitiveId(studentUserId, encryptionSalt);
            noticeUrl = noticeBaseUrl + notification.getNotificationId() + "?sid=" + encryptedStudentId;
        } else {
            noticeUrl = noticeBaseUrl + notification.getNotificationId();
        }

        String publishTime = formatPublishTime(notification.getCreateTime());

        String header;
        if (className != null && !className.isEmpty() && studentName != null && !studentName.isEmpty()) {
            header = "📢 您有一條 " + className + "-" + studentName + " 新的通告";
        } else {
            header = "📢 您有一條新的通告";
        }

        return header + "\n"
                + "──────────────\n"
                + "📌 標題：\n" + title + "\n\n"
                + "🕒 發佈時間：\n" + publishTime + "\n"
                + "──────────────\n"
                + "👉 請點擊以下連接查看詳情：\n" + noticeUrl;
    }

    /**
     * 創建發送記錄
     */
    private NotificationSendRecord createSendRecord(Notification notification,
                                                    List<String> studentUserIds,
                                                    SendResult sendResult,
                                                    List<SysDepartmentParentBinding> bindings) {
        NotificationSendRecord sendRecord = new NotificationSendRecord();
        sendRecord.setNotificationId(notification.getNotificationId());
        sendRecord.setSenderId(notification.getSenderId());
        sendRecord.setSenderName(notification.getSenderName());
        sendRecord.setSendTime(LocalDateTime.now());

        int initialCapacity = bindings == null ? 16 : (int) (bindings.size() / 0.75f) + 1;
        Map<String, Set<String>> studentParentMap = new HashMap<>(initialCapacity);
        if (bindings != null) {
            for (SysDepartmentParentBinding binding : bindings) {
                String studentId = binding.getStudentUserId();
                String parentId = binding.getParentUserId();
                if (studentId != null && parentId != null) {
                    studentParentMap.computeIfAbsent(studentId, k -> new HashSet<>()).add(parentId);
                }
            }
        }

        Set<String> allTargetStudents = new HashSet<>(studentParentMap.keySet());
        Set<String> studentUserIdsSet = Collections.emptySet();
        if (studentUserIds != null && !studentUserIds.isEmpty()) {
            studentUserIdsSet = new HashSet<>(studentUserIds);
            allTargetStudents.addAll(studentUserIdsSet);
        }

        int totalCount = allTargetStudents.size();
        int successCount = 0;
        int failCount = 0;

        Set<String> successUserIds = sendResult.getSuccessUserIds();

        for (String studentId : allTargetStudents) {
            boolean isSuccess = false;

            if (studentUserIdsSet.contains(studentId) && successUserIds.contains(studentId)) {
                isSuccess = true;
            } else {
                Set<String> parents = studentParentMap.get(studentId);
                if (parents != null) {
                    for (String pId : parents) {
                        if (successUserIds.contains(pId)) {
                            isSuccess = true;
                            break;
                        }
                    }
                }
            }

            if (isSuccess) {
                successCount++;
            } else {
                failCount++;
            }
        }

        sendRecord.setTotalCount(totalCount);
        sendRecord.setSuccessCount(successCount);
        sendRecord.setFailCount(failCount);

        if (failCount == 0 && totalCount > 0) {
            sendRecord.setSendStatus(SEND_STATUS_SUCCESS);
        } else if (successCount == 0 && totalCount > 0) {
            sendRecord.setSendStatus(SEND_STATUS_FAIL);
        } else {
            sendRecord.setSendStatus(SEND_STATUS_PARTIAL);
        }

        sendRecord.setCreateTime(LocalDateTime.now());

        return sendRecord;
    }

    /**
     * 創建用戶閱讀記錄列表
     */
    private List<NotificationUserReadRecord> createUserReadRecords(Long sendRecordId, List<String> parentUserIds,
                                                                   List<String> studentUserIds,
                                                                   Set<String> successUserIds,
                                                                   List<SysDepartmentParentBinding> bindings) {
        int capacity = (parentUserIds != null ? parentUserIds.size() : 0)
                + (studentUserIds != null ? studentUserIds.size() : 0);
        List<NotificationUserReadRecord> readRecords = new ArrayList<>(capacity);
        LocalDateTime now = LocalDateTime.now();

        int initialCapacity = bindings == null ? 16 : (int) (bindings.size() / 0.75f) + 1;
        Map<String, List<String>> parentToStudentsMap = new HashMap<>(initialCapacity);
        Set<String> parentStudentKeys = new HashSet<>();
        if (bindings != null) {
            for (SysDepartmentParentBinding binding : bindings) {
                if (binding.getParentUserId() != null && binding.getStudentUserId() != null) {
                    String key = binding.getParentUserId() + "_" + binding.getStudentUserId();
                    if (!parentStudentKeys.contains(key)) {
                        parentToStudentsMap.computeIfAbsent(binding.getParentUserId(), k -> new ArrayList<>())
                                .add(binding.getStudentUserId());
                        parentStudentKeys.add(key);
                    } else {
                        log.debug("createUserReadRecords: 跳過重複的綁定關係: parentUserId={}, studentUserId={}",
                                binding.getParentUserId(), binding.getStudentUserId());
                    }
                }
            }
        }

        if (parentUserIds != null) {
            for (String userId : parentUserIds) {
                List<String> studentIds = parentToStudentsMap.get(userId);
                boolean sendSuccess = successUserIds.contains(userId);

                if (studentIds != null && !studentIds.isEmpty()) {
                    for (String studentUserId : studentIds) {
                        readRecords.add(createReadRecord(sendRecordId, userId, "2", studentUserId, sendSuccess, now));
                    }
                } else {
                    readRecords.add(createReadRecord(sendRecordId, userId, "2", null, sendSuccess, now));
                }
            }
        }

        if (studentUserIds != null) {
            for (String userId : studentUserIds) {
                boolean sendSuccess = successUserIds.contains(userId);
                readRecords.add(createReadRecord(sendRecordId, userId, "1", userId, sendSuccess, now));
            }
        }

        return readRecords;
    }

    /**
     * 創建單條閱讀記錄
     */
    private NotificationUserReadRecord createReadRecord(Long sendRecordId, String userId, String userType,
                                                        String studentUserId, boolean sendSuccess, LocalDateTime createTime) {
        NotificationUserReadRecord record = new NotificationUserReadRecord();
        record.setSendRecordId(sendRecordId);
        record.setUserId(userId);
        record.setUserType(userType);
        record.setIsRead("0");
        record.setReplyStatus("0");
        record.setSendStatus(sendSuccess ? "1" : "0");
        record.setStudentUserId(studentUserId);
        record.setCreateTime(createTime);
        return record;
    }

    // =========================================================================
    // C. Private 撤回與抄送細節 (Private Recall & CC Flow Helpers)
    // =========================================================================

    /**
     * 獲取需要抄送的用戶 ID 集合（包括原抄送對象與系統管理員）
     */
    private Set<String> getCcUserIds(Long notificationId) {
        Set<String> allUserIds = new HashSet<>();

        // 1. 查詢該通知的抄送對象
        List<NotificationCc> ccs = notificationCcService.selectByNotificationId(notificationId);
        if (ccs != null && !ccs.isEmpty()) {
            allUserIds.addAll(notificationCcService.resolveCcUserIds(ccs));
        }

        // 2. 獲取所有狀態正常的管理員用戶 ID 列表
        List<String> adminUserIds = sysAdminMapper.selectAdminUserIds();
        if (adminUserIds != null && !adminUserIds.isEmpty()) {
            allUserIds.addAll(adminUserIds);
        }

        return allUserIds;
    }

    /**
     * 分批向原接收對象發送撤回通知
     */
    private void sendRecallToReceiversInBatches(List<String> parentUserIds, List<String> studentUserIds,
                                                List<String> partyIds, String content) {
        int parentBatches = calcBatchCount(parentUserIds.size(), PARENT_STUDENT_BATCH_SIZE);
        int studentBatches = calcBatchCount(studentUserIds.size(), PARENT_STUDENT_BATCH_SIZE);
        int partyBatches = calcBatchCount(partyIds.size(), PARTY_BATCH_SIZE);
        int totalBatches = Math.max(Math.max(parentBatches, studentBatches), partyBatches);

        for (int i = 0; i < totalBatches; i++) {
            BatchReceiversVO batch = getBatchData(parentUserIds, studentUserIds, partyIds, i);

            if (batch.isEmpty()) {
                continue;
            }

            try {
                JSONObject payload = buildSchoolNotificationPayload(batch.getParentIds(), batch.getStudentIds(), batch.getPartyIds(), content);
                wechatWorkHttpClient.sendSchoolNotification(payload);
            } catch (Exception e) {
                log.error("發送撤回微信通知第 {} 批異常 (原接收對象)", i + 1, e);
            }
        }
    }

    /**
     * 分批發送撤回應用消息（純文本）
     */
    private void sendRecallToCcInBatches(List<String> userIds, String content) {
        int totalBatches = calcBatchCount(userIds.size(), PARENT_STUDENT_BATCH_SIZE);

        for (int i = 0; i < totalBatches; i++) {
            List<String> currentUserIds = extractBatch(userIds, i, PARENT_STUDENT_BATCH_SIZE);

            if (currentUserIds.isEmpty()) {
                continue;
            }

            try {
                JSONObject payload = buildAppTextPayload(currentUserIds, content);
                wechatWorkHttpClient.sendAppMessage(payload);
            } catch (Exception e) {
                log.error("發送撤回微信通知第 {} 批異常 (抄送/管理員)", i + 1, e);
            }
        }
    }

    /**
     * 構建應用消息的純文本 Payload
     */
    private JSONObject buildAppTextPayload(List<String> userIds, String content) {
        JSONObject payload = new JSONObject();
        payload.put("touser", String.join("|", userIds));
        payload.put("msgtype", "text");
        payload.put("agentid", agentId);

        JSONObject text = new JSONObject();
        text.put("content", content);
        payload.put("text", text);

        payload.put("safe", 0);
        payload.put("enable_id_trans", 0);
        payload.put("enable_duplicate_check", 0);
        payload.put("duplicate_check_interval", DUPLICATE_CHECK_INTERVAL);

        return payload;
    }

    /**
     * 分批發送抄送消息
     */
    private void sendCcInBatches(Notification notification, List<String> userIds) {
        int totalBatches = calcBatchCount(userIds.size(), PARENT_STUDENT_BATCH_SIZE);

        log.info("通知 {} 的抄送消息需要分 {} 批發送，共 {} 個接收者",
                notification.getNotificationId(), totalBatches, userIds.size());

        for (int i = 0; i < totalBatches; i++) {
            List<String> currentUserIds = extractBatch(userIds, i, PARENT_STUDENT_BATCH_SIZE);

            if (currentUserIds.isEmpty()) {
                continue;
            }

            JSONObject payload = buildCcWechatPayload(currentUserIds, notification);

            log.info("發送通知 {} 的抄送消息第 {}/{} 批，接收者: {}",
                    notification.getNotificationId(), i + 1, totalBatches, currentUserIds.size());

            JSONObject result = wechatWorkHttpClient.sendAppMessage(payload);

            Integer errcode = result.getInteger("errcode");
            if (errcode == null || errcode != 0) {
                log.error("通知 {} 抄送消息第 {} 批發送失敗: code={}, msg={}",
                        notification.getNotificationId(), i + 1, errcode, result.getString("errmsg"));
                throw new IllegalStateException("企業微信抄送消息發送失敗（第 " + (i + 1) + " 批）: " + result.toJSONString());
            }

            log.info("通知 {} 抄送消息第 {}/{} 批發送成功", notification.getNotificationId(), i + 1, totalBatches);
        }

        log.info("通知 {} 的抄送消息已全部發送完成，共 {} 批", notification.getNotificationId(), totalBatches);
    }

    /**
     * 構建抄送消息的企業微信應用消息 Payload (文本卡片消息)
     */
    private JSONObject buildCcWechatPayload(List<String> userIds, Notification notification) {
        JSONObject payload = new JSONObject();

        String touser = String.join("|", userIds);
        payload.put("touser", touser);

        payload.put("msgtype", "textcard");
        payload.put("agentid", agentId);

        JSONObject textcard = new JSONObject();
        textcard.put("title", "📨 您有一條抄送的通知");

        String title = notification.getTitle() == null ? "" : notification.getTitle().trim();
        String publishTime = formatPublishTime(notification.getCreateTime());
        String description = "<div class=\"gray\">⏰ " + publishTime + "</div> "
                + "<div class=\"normal\">📋 " + title + "</div>";

        if (description.length() > 512) {
            description = description.substring(0, 512);
        }
        textcard.put("description", description);

        // 使用 WechatWorkHttpClient 動態組裝 WeChat OAuth URL
        String state = "campus_notice_" + notification.getNotificationId();
        String noticeUrl = ccNoticeBaseUrl + notification.getNotificationId();
        noticeUrl = wechatWorkHttpClient.buildOauthUrl(noticeUrl, state);
        textcard.put("url", noticeUrl);

        textcard.put("btntxt", "查看詳情");

        payload.put("textcard", textcard);

        payload.put("enable_id_trans", 0);
        payload.put("enable_duplicate_check", 0);
        payload.put("duplicate_check_interval", DUPLICATE_CHECK_INTERVAL);

        return payload;
    }

    // =========================================================================
    // D. Private 提醒細節 (Private Reminder Helpers)
    // =========================================================================

    /**
     * 構建提醒消息內容
     */
    private String buildRemindContent(Notification notification) {
        String title = notification.getTitle() == null ? "" : notification.getTitle().trim();
        String noticeUrl = noticeBaseUrl + notification.getNotificationId();

        String replyDeadline = notification.getReplyDeadline() != null
                ? notification.getReplyDeadline().format(DATE_FORMATTER)
                : "";

        return "🔔 溫馨提示\n" +
                "───────────────\n" +
                "您有一條通告需要回覆\n" +
                "───────────────\n" +
                "📌 標題：\n" + title + "\n\n" +
                "⏰ 回復截止時間：\n" + replyDeadline + "\n\n" +
                "👉 請點擊以下連接查看詳情：\n" + noticeUrl;
    }

    /**
     * 分批發送提醒消息
     */
    private boolean sendRemindInBatches(List<String> parentUserIds, String content) {
        if (parentUserIds == null || parentUserIds.isEmpty()) {
            return false;
        }

        int totalBatches = calcBatchCount(parentUserIds.size(), PARENT_STUDENT_BATCH_SIZE);

        for (int i = 0; i < totalBatches; i++) {
            List<String> currentBatch = extractBatch(parentUserIds, i, PARENT_STUDENT_BATCH_SIZE);

            if (currentBatch.isEmpty()) {
                continue;
            }

            JSONObject payload = buildParentOnlyPayload(currentBatch, content);

            try {
                JSONObject result = wechatWorkHttpClient.sendSchoolNotification(payload);
                Integer errcode = result.getInteger("errcode");

                if (errcode != null && errcode == 0) {
                    log.info("第 {}/{} 批提醒消息發送成功", i + 1, totalBatches);
                } else {
                    log.error("第 {}/{} 批提醒消息發送失敗: code={}, msg={}",
                            i + 1, totalBatches, errcode, result.getString("errmsg"));
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
     * 構建提醒記錄
     */
    private NotificationReminderRecord buildReminderRecord(Long notificationId, Long sendRecordId,
                                                           String studentUserId, String parentUserIdsStr,
                                                           LocalDateTime now, String status) {
        NotificationReminderRecord record = new NotificationReminderRecord();
        record.setNotificationId(notificationId);
        record.setSendRecordId(sendRecordId);
        record.setStudentUserId(studentUserId);
        record.setParentUserIds(parentUserIdsStr);
        record.setRemindSendTime(now);
        record.setRemindSendStatus(status);
        record.setCreateTime(now);
        return record;
    }

    // =========================================================================
    // E. Private 重發細節 (Private Resend Helpers)
    // =========================================================================

    /**
     * 批量更新閱讀記錄的發送狀態
     */
    private void updateReadRecords(List<NotificationUserReadRecord> records,
                                   String userType, Set<String> successUserIds) {
        for (NotificationUserReadRecord record : records) {
            if (userType.equals(record.getUserType())) {
                String newStatus = successUserIds.contains(record.getUserId()) ? "1" : "0";
                notificationUserReadRecordService.updateSendStatus(record.getReadId(), newStatus);
            }
        }
    }

    /**
     * 構建發送記錄更新對象
     */
    private NotificationSendRecord buildSendRecordUpdate(NotificationSendRecord sendRecord, int successDelta) {
        int newSuccessCount = (sendRecord.getSuccessCount() != null ? sendRecord.getSuccessCount() : 0) + successDelta;
        int newFailCount = (sendRecord.getFailCount() != null ? sendRecord.getFailCount() : 0) - successDelta;
        if (newFailCount < 0)
            newFailCount = 0;

        NotificationSendRecord updateRecord = new NotificationSendRecord();
        updateRecord.setSendRecordId(sendRecord.getSendRecordId());
        updateRecord.setSuccessCount(newSuccessCount);
        updateRecord.setFailCount(newFailCount);
        if (newFailCount == 0) {
            updateRecord.setSendStatus(SEND_STATUS_SUCCESS);
        } else if (newSuccessCount == 0) {
            updateRecord.setSendStatus(SEND_STATUS_FAIL);
        } else {
            updateRecord.setSendStatus(SEND_STATUS_PARTIAL);
        }
        updateRecord.setUpdateTime(LocalDateTime.now());
        return updateRecord;
    }

    // =========================================================================
    // F. Private 通用工具 (Private General Helpers)
    // =========================================================================

    /**
     * 獲取並組裝指定批次的數據
     */
    private BatchReceiversVO getBatchData(List<String> parentUserIds, List<String> studentUserIds, List<String> partyIds, int batchIndex) {
        List<String> currentParentIds = extractBatch(parentUserIds, batchIndex, PARENT_STUDENT_BATCH_SIZE);
        List<String> currentStudentIds = extractBatch(studentUserIds, batchIndex, PARENT_STUDENT_BATCH_SIZE);
        List<String> currentPartyIds = extractBatch(partyIds, batchIndex, PARTY_BATCH_SIZE);
        return new BatchReceiversVO(currentParentIds, currentStudentIds, currentPartyIds);
    }

    /**
     * 格式化發佈時間
     */
    private String formatPublishTime(LocalDateTime createTime) {
        return createTime != null ? createTime.format(DATE_FORMATTER) : "未知";
    }

    /**
     * 計算批次數量
     */
    private int calcBatchCount(int total, int batchSize) {
        return (int) Math.ceil((double) total / batchSize);
    }

    /**
     * 從列表中截取指定批次的數據
     */
    private List<String> extractBatch(List<String> list, int batchIndex, int batchSize) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        int fromIndex = batchIndex * batchSize;
        if (fromIndex >= list.size()) {
            return Collections.emptyList();
        }

        int toIndex = Math.min(fromIndex + batchSize, list.size());
        return list.subList(fromIndex, toIndex);
    }

    /**
     * List 轉 JSONArray
     */
    private JSONArray toJsonArray(List<String> values) {
        JSONArray array = new JSONArray();
        if (values != null && !values.isEmpty()) {
            array.addAll(values);
        }
        return array;
    }

    /**
     * Null 安全的 List 轉換
     */
    private <T> List<T> nullSafe(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }
}
