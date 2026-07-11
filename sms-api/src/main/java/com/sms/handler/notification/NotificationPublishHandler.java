package com.sms.handler.notification;

import com.alibaba.fastjson.JSONObject;
import com.sms.framework.wechat.WechatWorkHttpClient;
import com.sms.system.entity.task.TaskResult;
import com.sms.system.entity.dto.NotificationQueryDTO;
import com.sms.system.entity.notification.*;
import com.sms.handler.notification.NotificationCcSendHelper;
import com.sms.handler.notification.NotificationMessageContentHelper;
import com.sms.handler.notification.NotificationSchoolSendHelper;
import com.sms.system.entity.vo.ResolvedReceiversVO;
import com.sms.system.entity.notification.receiver.NotificationReceiverStats;
import com.sms.system.entity.vo.ParentStudentMessageInfo;
import com.sms.system.entity.vo.UnrepliedStudentVO;
import com.sms.system.entity.vo.NotificationVO;
import com.sms.system.service.notification.*;
import com.sms.common.utils.bean.BeanCopyUtils;
import com.sms.system.service.INotificationMessageService;
import com.sms.system.service.ISysSchoolFamilyContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通告發佈處理器
 * 負責將通告消息推送給指定的接收者（如通過企業微信發送家校通知）
 */
@Component
public class NotificationPublishHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublishHandler.class);

    /**
     * 學生手冊的查看基礎 URL。
     */
    @Value("${wechat.work.handbookBaseUrl:http://10.32.96.55:8080/handbook}")
    private String handbookBaseUrl;

    @Autowired
    private NotificationSchoolSendHelper schoolSendHelper;

    @Autowired
    private NotificationCcSendHelper ccSendHelper;

    @Autowired
    private NotificationMessageContentHelper messageContentHelper;

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
    private INotificationService notificationService;

    @Autowired
    private INotificationPublishRecordService notificationPublishRecordService;

    @Autowired
    private INotificationResendFailRecordService notificationResendFailRecordService;

    @Autowired
    private INotificationMessageService notificationMessageService;

    @Autowired
    private ISysSchoolFamilyContactService schoolFamilyContactService;

    // =========================================================================
    // A. Public 入口方法 (Public API)
    // =========================================================================

    /**
     * 保存後發佈通知（從數據庫加載實體後推送）
     */
    public void publishAfterSave(Long notificationId) {
        Notification notification = loadNotificationEntity(notificationId);
        List<NotificationReceiver> receivers = loadReceivers(notificationId);
        publishToWechat(notification, receivers);
        sendCcNotifications(notification);
    }

    /**
     * 發送撤回通知（通過通知 ID）
     */
    public void sendRecallNotification(Long notificationId) {
        Notification notification = loadNotificationEntity(notificationId);
        if (notification != null) {
            sendRecallNotification(notification);
        }
    }

    /**
     * 將通告發佈到企業微信家校通知（主接收人，不含抄送）。
     * <p>流程：解析接收人 → 構建個性化消息 → 分批發送 → 持久化發送/閱讀記錄。</p>
     *
     * @param notification 通告實體
     * @param receivers    前端保存的接收對象配置（班級/個人、企微/自定義家校）
     * @throws IllegalStateException 未解析出有效接收者時拋出
     */
    public void publishToWechat(Notification notification, List<NotificationReceiver> receivers) {
        ResolvedReceiversVO resolved = notificationReceiverService.resolveReceivers(receivers, true);

        if (!resolved.hasAnyReceiver()) {
            log.warn("未找到有效的微信收件人進行通知 {}", notification.getNotificationId());
            throw new IllegalStateException("未解析出有效的微信接收者");
        }

        List<ParentStudentMessageInfo> messageInfos =
                notificationMessageService.buildMessageInfos(
                        resolved.getReceiverTargets(), resolved.getRelations());

        SendResult sendResult = schoolSendHelper.sendWithPersonalization(
                notification, resolved.getParentUserIds(), messageInfos);

        notificationPublishRecordService.savePublishRecords(
                notification, sendResult, resolved.getReceiverTargets());
    }

    /**
     * 發送抄送通知到企業微信應用消息
     *
     * @param notification 通告實體對象
     */
    public void sendCcNotifications(Notification notification) {
        Set<String> allUserIds = ccSendHelper.resolveCcRecipientUserIds(
                loadCcs(notification.getNotificationId()));

        if (allUserIds.isEmpty()) {
            log.info("通知 {} 沒有設置抄送對象且無管理員配置", notification.getNotificationId());
            return;
        }

        ccSendHelper.sendCcInBatches(notification, new ArrayList<>(allUserIds));
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

        List<NotificationReceiver> receivers = loadReceivers(originalNotification.getNotificationId());
        ResolvedReceiversVO resolved = notificationReceiverService.resolveReceivers(receivers, false);
        String recallContent = messageContentHelper.buildRecallContent(originalNotification);

        if (resolved.hasAnyReceiver()) {
            schoolSendHelper.sendRecallInBatches(resolved.getParentUserIds(), recallContent);
        }

        Set<String> ccUserIds = ccSendHelper.resolveCcRecipientUserIds(
                loadCcs(originalNotification.getNotificationId()));
        if (!ccUserIds.isEmpty()) {
            ccSendHelper.sendRecallTextInBatches(new ArrayList<>(ccUserIds), recallContent);
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
        Notification notification = loadNotificationEntity(notificationId);
        if (notification == null) {
            throw new IllegalStateException("未找到通知資訊");
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
        String remindContent = messageContentHelper.buildRemindContent(notification);

        // 6. 爲每個未回復的學生發送提醒通知
        for (UnrepliedStudentVO student : unrepliedStudents) {
            String studentId = student.getStudentId();
            List<String> parentUserIdList = student.getParentUserIds();

            if (studentId == null || parentUserIdList == null || parentUserIdList.isEmpty()) {
                continue;
            }

            // 直接使用 List 的 toString() 方法存儲爲字符串
            String parentUserIdsStr = parentUserIdList.toString();

            try {
                // 分批發送提醒消息
                boolean sendSuccess = schoolSendHelper.sendRemindInBatches(parentUserIdList, remindContent);

                // 建立提醒記錄
                reminderRecords.add(buildReminderRecord(
                        notificationId, sendRecord.getSendRecordId(),
                        studentId, parentUserIdsStr, now, sendSuccess ? "1" : "2"));

                if (sendSuccess) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                log.error("發送提醒通知失敗，學生ID: {}", studentId, e);
                failCount++;

                // 即使失敗也建立記錄
                reminderRecords.add(buildReminderRecord(
                        notificationId, sendRecord.getSendRecordId(),
                        studentId, parentUserIdsStr, now, "2"));
            }
        }

        // 7. 批量保存提醒記錄
        notificationPublishRecordService.saveReminderRecords(reminderRecords);

        // 8. 構建返回結果
        result.put("success", true);
        result.put("remindCount", unrepliedStudents.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);

        // 根據發送結果生成不同的提示資訊
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
        Notification notification = loadNotificationEntity(notificationId);
        if (notification == null) {
            throw new IllegalStateException("未找到通知資訊");
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

        // 5. 收集需重發的家長 userid（user_type=1 學生/家長）
        Set<String> failedParentIdSet = new LinkedHashSet<>();
        for (NotificationUserReadRecord record : failedRecords) {
            if (record.getUserId() == null || record.getUserId().trim().isEmpty()) {
                continue;
            }
            if ("1".equals(record.getUserType())) {
                failedParentIdSet.add(record.getUserId().trim());
            }
        }
        List<String> failedParentIds = new ArrayList<>(failedParentIdSet);

        // 6. 重新發送
        Set<String> overallSuccessUserIds = new HashSet<>();
        Map<String, String> allFailedUserReasons = new HashMap<>();

        if (!failedParentIds.isEmpty()) {
            SendResult parentResult = schoolSendHelper.sendInBatches(
                    notification, failedParentIds);
            overallSuccessUserIds.addAll(parentResult.getSuccessUserIds());
            if (parentResult.getFailedUserReasons() != null) {
                allFailedUserReasons.putAll(parentResult.getFailedUserReasons());
            }
        }

        // 7. 以學籍 student_id 為維度統計成功/失敗數
        Map<String, Set<String>> studentToParentsMap =
                NotificationReceiverStats.groupParentsByStudentFromReadRecords(failedRecords);
        int[] counts = NotificationReceiverStats.countStudentResults(studentToParentsMap, overallSuccessUserIds);
        int successCount = counts[0];
        int failCount = counts[1];

        // 8. 持久化重發結果
        notificationPublishRecordService.saveResendRecords(
                notificationId, sendRecord, failedRecords, overallSuccessUserIds,
                allFailedUserReasons, isAutoTask, successCount);

        // 9. 構建返回結果
        result.put("resendCount", studentToParentsMap.size());
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
     * 每日學生手冊通知發送主方法
     * 由 SchoolNoticeTask 調用（每周一至周五下午 6 點）
     */
    public TaskResult sendDailySchoolNotice() {
        log.info("開始執行學校通知發送任務");
        // 按基礎設置中配置的學段，查詢該學段下家長 userid
        List<String> allParentUserIds = schoolFamilyContactService.getAllParentUserIds();
        if (allParentUserIds == null || allParentUserIds.isEmpty()) {
            log.warn("沒有可發送的家長用戶（請確認基礎設置已選擇每日學生手冊通知班級，且對應班級下已有家校聯絡人數據）");
            return TaskResult.success(0, 0, "無家長需發送");
        }
        log.info("獲取到家長用戶 ID 總數量: {}", allParentUserIds.size());

        int totalBatches = NotificationSchoolSendHelper.calcBatchCount(
                allParentUserIds.size(),
                NotificationSchoolSendHelper.PARENT_BATCH_SIZE);
        log.info("需要分 {} 批發送，每批最多 {} 個家長", totalBatches,
                NotificationSchoolSendHelper.PARENT_BATCH_SIZE);

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMsg = new StringBuilder();

        for (int i = 0; i < allParentUserIds.size(); i += NotificationSchoolSendHelper.PARENT_BATCH_SIZE) {
            int endIndex = Math.min(i + NotificationSchoolSendHelper.PARENT_BATCH_SIZE, allParentUserIds.size());
            List<String> batchList = allParentUserIds.subList(i, endIndex);
            int batchNumber = (i / NotificationSchoolSendHelper.PARENT_BATCH_SIZE) + 1;

            log.info("開始發送第 {}/{} 批，本批家長數量: {}", batchNumber, totalBatches, batchList.size());

            String content = "📚 今日學生手冊\n\n"
                    + "🔗 請點擊以下連接查看今日學生手冊：\n"
                    + handbookBaseUrl;

            JSONObject result = wechatWorkHttpClient.sendSchoolNotification(
                    schoolSendHelper.buildParentOnlyPayload(batchList, content));

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

        NotificationQueryDTO queryParam = new NotificationQueryDTO();
        queryParam.setStatus("1");
        queryParam.setReminderTime(LocalDateTime.now());
        List<Notification> notificationList = BeanCopyUtils.copyList(
                notificationService.selectNotificationList(queryParam), Notification.class);

        if (notificationList.isEmpty()) {
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
            return new TaskResult(remindCount, failCount, "定時提示家長回復通知部分失敗: " + errorMsg);
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

    // -------------------------------------------------------------------------
    // 數據加載與提醒記錄構建（Handler 內部共用）
    // -------------------------------------------------------------------------

    /**
     * 構建單條「提示家長回覆」記錄，寫入 notification_reminder_record。
     * <p>按學生維度記錄一次提醒：關聯原發送記錄、學生 ID、本次提醒的家長列表及發送結果。</p>
     *
     * @param status 提醒發送狀態（1=成功，2=失敗）
     */
    private NotificationReminderRecord buildReminderRecord(Long notificationId, Long sendRecordId,
                                                           String studentId, String parentUserIdsStr,
                                                           LocalDateTime now, String status) {
        NotificationReminderRecord record = new NotificationReminderRecord();
        record.setNotificationId(notificationId);
        record.setSendRecordId(sendRecordId);
        record.setStudentId(studentId);
        record.setParentUserIds(parentUserIdsStr);
        record.setRemindSendTime(now);
        record.setRemindSendStatus(status);
        record.setCreateTime(now);
        return record;
    }

    /**
     * 按通知 ID 從數據庫加載通知實體（VO 轉 Entity）。
     * <p>用於發佈、撤回、重發、提醒等流程，避免 Handler 各入口重複查詢主表。</p>
     */
    private Notification loadNotificationEntity(Long notificationId) {
        NotificationVO vo = notificationService.selectNotificationById(notificationId);
        return BeanCopyUtils.copy(vo, Notification.class);
    }

    /**
     * 加載通知的接收對象配置（notification_receiver 表）。
     * <p>發佈、撤回時需重新解析 receive_data，還原當初選中的班級/個人並轉成家長 userid。</p>
     */
    private List<NotificationReceiver> loadReceivers(Long notificationId) {
        return BeanCopyUtils.copyList(notificationReceiverService.selectByNotificationId(notificationId),
                NotificationReceiver.class);
    }

    /**
     * 加載通知的抄送對象配置（notification_cc 表）。
     * <p>發佈後抄送、撤回通知抄送對象時使用，再交由 {@link NotificationCcSendHelper} 解析為企微 userid。</p>
     */
    private List<NotificationCc> loadCcs(Long notificationId) {
        return BeanCopyUtils.copyList(notificationCcService.selectByNotificationId(notificationId), NotificationCc.class);
    }
}
