package com.sms.handler.notification;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.common.utils.security.Md5Utils;
import com.sms.framework.wechat.WechatWorkHttpClient;
import com.sms.system.entity.task.TaskResult;
import com.sms.system.entity.notification.*;
import com.sms.system.entity.vo.ResolvedReceiversVO;
import com.sms.system.entity.vo.ParentStudentMessageInfo;
import com.sms.system.service.notification.*;
import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.entity.vo.UnrepliedStudentVO;
import com.sms.system.service.INotificationMessageService;
import com.sms.system.service.ISysDepartmentParentBindingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
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
    private static final String SEND_STATUS_FAIL    = "3";
    private static final String SEND_STATUS_PARTIAL = "4";
    // 線程安全的日期格式化器
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

    @Value("${wechat.work.agentId:#{null}}")
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

        List<String> parentUserIds  = nullSafe(resolvedReceivers.getParentUserIds());
        List<String> studentUserIds  = nullSafe(resolvedReceivers.getStudentUserIds());
        List<String> partyIds        = nullSafe(resolvedReceivers.getPartyIds());
        List<SysDepartmentParentBinding> bindings = nullSafe(resolvedReceivers.getBindings());

        // 如果沒有任何接收者，則拋出異常避免無效調用
        if (parentUserIds.isEmpty() && studentUserIds.isEmpty() && partyIds.isEmpty()) {
            log.warn("未找到有效的微信收件人進行通知 {}", notification.getNotificationId());
            throw new IllegalStateException("未解析出有效的微信接收者");
        }

        // 2. 使用 Service 批量構建消息信息（包含班級名和學生名）
        List<ParentStudentMessageInfo> messageInfos = notificationMessageService.buildMessageInfos(bindings);

        // 3. 分批發送通知，並獲取發送結果
        SendResult sendResult = sendInBatchesWithPersonalization(notification, parentUserIds, studentUserIds, partyIds, messageInfos);

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
        // 1. 查詢該通知的抄送對象
        List<NotificationCc> ccs = notificationCcService.selectByNotificationId(notification.getNotificationId());
        
        if (ccs == null || ccs.isEmpty()) {
            log.info("通知 {} 沒有設置抄送對象", notification.getNotificationId());
            return;
        }
        
        // 2. 使用 Service 解析所有抄送對象，獲取 userid 列表
        Set<String> allUserIds = notificationCcService.resolveCcUserIds(ccs);
        
        // 3. 如果沒有有效的 userid，直接返回
        if (allUserIds.isEmpty()) {
            log.warn("通知 {} 的抄送對象中沒有解析出有效的 userid", notification.getNotificationId());
            return;
        }
        
        // 4. 分批發送抄送消息
        sendCcInBatches(notification, new ArrayList<>(allUserIds));
    }

    /**
     * 構建發送給企業微信接口的 JSON 數據實體
     */
    private JSONObject buildWechatPayload(List<String> parentUserIds, List<String> studentUserIds, List<String> partyIds, Notification notification) {
        JSONObject payload = new JSONObject();
        // recv_scope: 0表示發送給指定的家長、學生或部門
        payload.put("recv_scope", 0);
        
        // 設置接收者，修復了原代碼中忽視 studentUserIds 和 partyIds 的邏輯
        payload.put("to_parent_userid", toJsonArray(parentUserIds));
        payload.put("to_student_userid", toJsonArray(studentUserIds));
        payload.put("to_party", toJsonArray(partyIds));
        
        // toall: 0表示不發送給所有人
        payload.put("toall", 0);
        
        // 消息類型與應用ID
        payload.put("msgtype", "text");
        payload.put("agentid", agentId);

        // 構建文本內容
        JSONObject text = new JSONObject();
        text.put("content", buildContent(notification));
        payload.put("text", text);

        // 其他發送配置
        payload.put("enable_id_trans", 0);
        payload.put("enable_duplicate_check", 0);
        payload.put("duplicate_check_interval", DUPLICATE_CHECK_INTERVAL);
        
        return payload;
    }

    /**
     * 構建消息文本內容
     *
     * @param notification 通告實體
     * @return 格式化後的文本內容
     */
    private String buildContent(Notification notification) {
        return buildContent(notification, null, null, null);
    }

    /**
     * 構建個性化消息文本內容（帶班級名和學生名）
     *
     * @param notification 通告實體
     * @param className 班級名稱
     * @param studentName 學生姓名
     * @param studentUserId 學生用戶 ID（用於構建加密的跳轉鏈接）
     * @return 格式化後的文本內容
     */
    private String buildContent(Notification notification, String className, String studentName, String studentUserId) {
        // 標題
        String title = notification.getTitle() == null ? "" : notification.getTitle().trim();
        // 設置跳轉地址
        String noticeUrl = notification.getJumpUrl();
        
        // 如果通告沒有自定義的跳轉鏈接，則使用默認的詳情頁鏈接
        if (noticeUrl == null || noticeUrl.trim().isEmpty()) {
            // 如果有學生用戶 ID，則將其加密後附加到 URL 中
            if (studentUserId != null && !studentUserId.trim().isEmpty()) {
                // 加密學生用戶 ID
                String encryptedStudentId = Md5Utils.encryptSensitiveId(studentUserId, encryptionSalt);
                // 組成跳轉鏈接
                noticeUrl = noticeBaseUrl + notification.getNotificationId() + "?sid=" + encryptedStudentId;
            } else {
                noticeUrl = noticeBaseUrl + notification.getNotificationId();
            }
        }
        
        // 格式化發佈時間
        String publishTime = formatPublishTime(notification.getCreateTime());
        
        // 構建開頭文字：如果有班級和學生信息，則顯示個性化消息
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
     * 分批發送通知，根據企業微信 API 的人數限制進行分批
     * - to_parent_userid 和 to_student_userid: 每批最多 1000 個
     * - to_party: 每批最多 100 個
     *
     * @param notification   通知實體
     * @param parentUserIds  家長用戶 ID 列表
     * @param studentUserIds 學生用戶 ID 列表
     * @param partyIds       部門 ID 列表
     * @return 發送結果（成功數、失敗數）
     */
    private SendResult sendInBatches(Notification notification, List<String> parentUserIds, 
                               List<String> studentUserIds, List<String> partyIds) {
        // 計算需要的批次數量
        int parentBatches  = calcBatchCount(parentUserIds.size(), PARENT_STUDENT_BATCH_SIZE);
        int studentBatches = calcBatchCount(studentUserIds.size(), PARENT_STUDENT_BATCH_SIZE);
        int partyBatches   = calcBatchCount(partyIds.size(), PARTY_BATCH_SIZE);
        int totalBatches   = Math.max(Math.max(parentBatches, studentBatches), partyBatches);
        
        log.info("通知 {} 需要分 {} 批發送（家長 {} 批，學生 {} 批，部門 {} 批）",
                notification.getNotificationId(), totalBatches, parentBatches, studentBatches, partyBatches);

        int successCount = 0;
        int failCount = 0;
        // 用於記錄成功接收到通知的用戶 ID（家長 + 學生）
        Set<String> successUserIds = new HashSet<>();
        // 用于记录失败用户的失败原因
        Map<String, String> failedUserReasons = new HashMap<>();

        // 分批發送
        for (int i = 0; i < totalBatches; i++) {
            // 截取當前批次的數據
            List<String> currentParentIds  = extractBatch(parentUserIds, i, PARENT_STUDENT_BATCH_SIZE);
            List<String> currentStudentIds = extractBatch(studentUserIds, i, PARENT_STUDENT_BATCH_SIZE);
            List<String> currentPartyIds   = extractBatch(partyIds, i, PARTY_BATCH_SIZE);
        
            // 如果當前批次沒有任何接收者，跳過
            if (currentParentIds.isEmpty() && currentStudentIds.isEmpty() && currentPartyIds.isEmpty()) {
                continue;
            }
        
            // 構建並發送當前批次的消息
            JSONObject payload = buildWechatPayload(currentParentIds, currentStudentIds, currentPartyIds, notification);
                    
            log.info("發送通知 {} 的第 {}/{} 批，家長: {}, 學生: {}, 部門: {}",
                    notification.getNotificationId(), i + 1, totalBatches,
                    currentParentIds.size(), currentStudentIds.size(), currentPartyIds.size());
                    
            JSONObject result = wechatWorkHttpClient.sendSchoolNotification(payload);
                    
            Integer errcode = result.getInteger("errcode");
            // 如果返回的錯誤碼不是 0，則表示發送失敗
            if (errcode == null || errcode != 0) {
                String errmsg = result.getString("errmsg");
                log.error("通知 {} 第 {} 批發送失敗: code={}, msg={}",
                        notification.getNotificationId(), i + 1, errcode, errmsg);
                failCount += currentParentIds.size() + currentStudentIds.size();
                
                String reason = "接口返回错误: " + errcode;
                for (String uid : currentParentIds) failedUserReasons.put(uid, reason);
                for (String uid : currentStudentIds) failedUserReasons.put(uid, reason);
            } else {
                log.info("通知 {} 第 {}/{} 批發送成功", notification.getNotificationId(), i + 1, totalBatches);
                
                Set<String> batchSuccessUsers = new HashSet<>(currentParentIds.size() + currentStudentIds.size());
                batchSuccessUsers.addAll(currentParentIds);
                batchSuccessUsers.addAll(currentStudentIds);

                // 解析 invaliduser (部分常規 API 返回字串形式)
                String invaliduser = result.getString("invaliduser");
                if (invaliduser != null && !invaliduser.isEmpty()) {
                    String[] invalidUsers = invaliduser.split("\\|");
                    for (String invalidId : invalidUsers) {
                        batchSuccessUsers.remove(invalidId);
                    }
                }

                // 解析 invalid_parent_userid (家校消息 API 返回 JSON 數組形式)
                JSONArray invalidParents = result.getJSONArray("invalid_parent_userid");
                if (invalidParents != null) {
                    for (int j = 0; j < invalidParents.size(); j++) {
                        batchSuccessUsers.remove(invalidParents.getString(j));
                    }
                }

                // 解析 invalid_student_userid
                JSONArray invalidStudents = result.getJSONArray("invalid_student_userid");
                if (invalidStudents != null) {
                    for (int j = 0; j < invalidStudents.size(); j++) {
                        batchSuccessUsers.remove(invalidStudents.getString(j));
                    }
                }
                
                // 收集失败用户及原因
                Set<String> batchFailedUsers = new HashSet<>(currentParentIds);
                batchFailedUsers.addAll(currentStudentIds);
                batchFailedUsers.removeAll(batchSuccessUsers);
                for (String failedId : batchFailedUsers) {
                    failedUserReasons.put(failedId, "无效用户或微信端未关注");
                }
                
                int batchTotal = currentParentIds.size() + currentStudentIds.size();
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
     * 從列表中截取指定批次的數據
     *
     * @param list      原始列表
     * @param batchIndex 批次索引（從 0 開始）
     * @param batchSize  每批大小
     * @return 當前批次的子列表
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
     * 構建發送給企業微信接口的 JSON 數據實體
     */
    private JSONArray toJsonArray(List<String> values) {
        JSONArray array = new JSONArray();
        if (values != null && !values.isEmpty()) {
            array.addAll(values);
        }
        return array;
    }

    /**
     * 格式化發佈時間
     *
     * @param createTime 創建時間
     * @return 格式化後的時間字符串 (yyyy-MM-dd HH:mm:ss)
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
     * Null 安全的 List 轉換
     */
    private <T> List<T> nullSafe(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }
    
    /**
     * 分批發送抄送消息
     *
     * @param notification 通知實體
     * @param userIds      接收者 userid 列表
     */
    private void sendCcInBatches(Notification notification, List<String> userIds) {
        int totalBatches = calcBatchCount(userIds.size(), PARENT_STUDENT_BATCH_SIZE);
        
        log.info("通知 {} 的抄送消息需要分 {} 批發送，共 {} 個接收者",
                notification.getNotificationId(), totalBatches, userIds.size());
        
        for (int i = 0; i < totalBatches; i++) {
            List<String> currentUserIds = extractBatch(userIds, i, PARENT_STUDENT_BATCH_SIZE);
            
            // 如果當前批次沒有任何接收者，跳過
            if (currentUserIds.isEmpty()) {
                continue;
            }
            
            // 構建並發送當前批次的消息
            JSONObject payload = buildCcWechatPayload(currentUserIds, notification);
            
            log.info("發送通知 {} 的抄送消息第 {}/{} 批，接收者: {}",
                    notification.getNotificationId(), i + 1, totalBatches, currentUserIds.size());
            
            JSONObject result = wechatWorkHttpClient.sendAppMessage(payload);
            
            Integer errcode = result.getInteger("errcode");
            // 如果返回的錯誤碼不是 0，則表示發送失敗
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
     *
     * @param userIds      接收者 userid 列表
     * @param notification 通知實體
     * @return 企業微信應用消息 JSON
     */
    private JSONObject buildCcWechatPayload(List<String> userIds, Notification notification) {
        JSONObject payload = new JSONObject();
        
        // 設置接收者，多個 userid 用 '|' 分隔
        String touser = String.join("|", userIds);
        payload.put("touser", touser);
        
        // 消息類型與應用ID
        payload.put("msgtype", "textcard");
        payload.put("agentid", agentId);
        
        // 構建文本卡片內容
        JSONObject textcard = new JSONObject();
        
        // 標題 (固定文字)
        textcard.put("title", "📨 您有一條抄送的通知");
        
        // 描述 (支持 HTML 格式) - 顯示通知標題和發佈時間
        String title = notification.getTitle() == null ? "" : notification.getTitle().trim();
        // 發佈時間
        String publishTime = formatPublishTime(notification.getCreateTime());
        // 內容
        String description = "<div class=\"gray\">⏰ " + publishTime + "</div> "
            + "<div class=\"normal\">📋 " + title + "</div>";
        // 截取前 512 個字符
        if (description.length() > 512) {
            description = description.substring(0, 512);
        }
        // 設置描述
        textcard.put("description", description);
        // 跳轉鏈接 - 抄送通知跳轉到抄送列表詳情頁
        String noticeUrl = notification.getJumpUrl();
        if (noticeUrl == null || noticeUrl.trim().isEmpty()) {
            // 使用抄送通知專用的基礎 URL，並傳遞通知ID作為參數
            noticeUrl = ccNoticeBaseUrl + notification.getNotificationId();
        }
        textcard.put("url", noticeUrl);
        
        // 按鈕文字
        textcard.put("btntxt", "查看詳情");
        
        payload.put("textcard", textcard);
        
        // 其他發送配置
        payload.put("enable_id_trans", 0);
        payload.put("enable_duplicate_check", 0);
        payload.put("duplicate_check_interval", DUPLICATE_CHECK_INTERVAL);
        
        return payload;
    }

    /**
     * 創建發送記錄
     *
     * @param notification   通知實體
     * @param studentUserIds 學生用戶 ID 列表
     * @param sendResult     發送結果
     * @param bindings       家長學生綁定關係列表
     * @return 發送記錄
     */
    private NotificationSendRecord createSendRecord(Notification notification,
                                                    List<String> studentUserIds,
                                                    SendResult sendResult,
                                                    List<SysDepartmentParentBinding> bindings) {
        // 發送記錄
        NotificationSendRecord sendRecord = new NotificationSendRecord();
        sendRecord.setNotificationId(notification.getNotificationId());
        sendRecord.setSenderId(notification.getSenderId());
        sendRecord.setSenderName(notification.getSenderName());
        sendRecord.setSendTime(LocalDateTime.now());
        
        // --- 重構統計邏輯，以 student_user_id 爲維度統計 ---
        
        // 1. 建立 student_user_id 及其對應的 parentUserId 集合映射
        int initialCapacity = bindings == null ? 16 : (int) (bindings.size() / 0.75f) + 1;
        Map<String, Set<String>> studentParentMap = new HashMap<>(initialCapacity);
        // 如果存在家長學生綁定關係
        if (bindings != null) {
            // 遍歷所有家長學生綁定關係
            for (SysDepartmentParentBinding binding : bindings) {
                String studentId = binding.getStudentUserId();
                String parentId = binding.getParentUserId();
                if (studentId != null && parentId != null) {
                    studentParentMap.computeIfAbsent(studentId, k -> new HashSet<>()).add(parentId);
                }
            }
        }
        
        // 3. 記錄所有被發送的 unique student_user_id (包括直接發送的學生和家長對應的學生)
        // 所有被發送的學生
        Set<String> allTargetStudents = new HashSet<>(studentParentMap.keySet());
        // 如果存在 student_user_id，則將它們加入到 allTargetStudents 中
        Set<String> studentUserIdsSet = Collections.emptySet();
        if (studentUserIds != null && !studentUserIds.isEmpty()) {
            studentUserIdsSet = new HashSet<>(studentUserIds);
            allTargetStudents.addAll(studentUserIdsSet);
        }
        
        // 總數 = 獨立學生數
        int totalCount = allTargetStudents.size();
        int successCount = 0;
        int failCount = 0;
        
        Set<String> successUserIds = sendResult.getSuccessUserIds();
        
        // 4. 統計成功和失敗的數量 (只要有一個對應的微信帳號發送成功，就視為該學生通知成功)
        for (String studentId : allTargetStudents) {
            boolean isSuccess = false;
            
            // 檢查直接發送給學生的消息是否成功 (改用 Set 的 contains 提升效能至 O(1))
            if (studentUserIdsSet.contains(studentId) && successUserIds.contains(studentId)) {
                isSuccess = true;
            } else {
                // 檢查發送給該學生家長的消息是否有任何一個成功
                Set<String> parents = studentParentMap.get(studentId);
                // 如果存在家長
                if (parents != null) {
                    // 檢查家長是否成功
                    for (String pId : parents) {
                        // 如果家長成功
                        if (successUserIds.contains(pId)) {
                            // 如果家長成功，則標記學生成功
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
        
        // 設置發送狀態：全部成功=2，全部失敗=3，部分成功=4
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
     *
     * @param sendRecordId   發送記錄 ID
     * @param parentUserIds  家長用戶 ID 列表
     * @param studentUserIds 學生用戶 ID 列表
     * @param successUserIds 企業微信發送成功的用戶 ID 集合
     * @param bindings       家長學生綁定關係列表
     * @return 閱讀記錄列表
     */
    private List<NotificationUserReadRecord> createUserReadRecords(Long sendRecordId, List<String> parentUserIds,
                                                                   List<String> studentUserIds,
                                                                   Set<String> successUserIds,
                                                                   List<SysDepartmentParentBinding> bindings) {
        // 用戶閱讀記錄列表，預分配容量避免陣列擴容
        int capacity = (parentUserIds != null ? parentUserIds.size() : 0) + (studentUserIds != null ? studentUserIds.size() : 0);
        List<NotificationUserReadRecord> readRecords = new ArrayList<>(capacity);
        LocalDateTime now = LocalDateTime.now();
        
        // 建立 parentUserId -> 所有對應的 studentUserId 列表的映射（一個家長可能綁定多個學生）
        int initialCapacity = bindings == null ? 16 : (int) (bindings.size() / 0.75f) + 1;
        Map<String, List<String>> parentToStudentsMap = new HashMap<>(initialCapacity);
        Set<String> parentStudentKeys = new HashSet<>(); // 用於去重
        if (bindings != null) {
            for (SysDepartmentParentBinding binding : bindings) {
                if (binding.getParentUserId() != null && binding.getStudentUserId() != null) {
                    // 去重：如果已經存在相同的 parentUserId + studentUserId 組合，則跳過
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
        
        // 為每個家長創建閱讀記錄（user_type = 2）
        // 關鍵修復：如果一個家長綁定了多個學生，需要為每個學生創建獨立的閱讀記錄
        if (parentUserIds != null) {
            for (String userId : parentUserIds) {
                List<String> studentIds = parentToStudentsMap.get(userId);
                boolean sendSuccess = successUserIds.contains(userId);
                
                // 如果該家長有綁定的學生，為每個學生創建一條記錄
                if (studentIds != null && !studentIds.isEmpty()) {
                    for (String studentUserId : studentIds) {
                        readRecords.add(createReadRecord(sendRecordId, userId, "2", studentUserId, sendSuccess, now));
                    }
                } else {
                    // 如果沒有綁定關係，也創建一條記錄（studentUserId 為 null）
                    readRecords.add(createReadRecord(sendRecordId, userId, "2", null, sendSuccess, now));
                }
            }
        }

        // 為每個學生創建閱讀記錄（user_type = 1）
        if (studentUserIds != null) {
            for (String userId : studentUserIds) {
                boolean sendSuccess = successUserIds.contains(userId);
                // 學生本身的 studentUserId 就是自己
                readRecords.add(createReadRecord(sendRecordId, userId, "1", userId, sendSuccess, now));
            }
        }

        return readRecords;
    }

    /**
     * 創建單條閱讀記錄
     *
     * @param sendRecordId   發送記錄 ID
     * @param userId         用戶 ID
     * @param userType       用戶類型（1-學生，2-家長）
     * @param studentUserId  關聯的學生 ID
     * @param sendSuccess    是否發送成功
     * @param createTime     創建時間
     * @return 閱讀記錄
     */
    private NotificationUserReadRecord createReadRecord(Long sendRecordId, String userId, String userType,
                                                         String studentUserId, boolean sendSuccess, LocalDateTime createTime) {
        NotificationUserReadRecord record = new NotificationUserReadRecord();
        record.setSendRecordId(sendRecordId);
        record.setUserId(userId);
        record.setUserType(userType);
        record.setIsRead("0"); // 0-未讀
        record.setReplyStatus("0"); // 0-未回覆
        record.setSendStatus(sendSuccess ? "1" : "0"); // 1-成功，0-失敗
        record.setStudentUserId(studentUserId);
        record.setCreateTime(createTime);
        return record;
    }

    /**
     * 提示家长回复（重新发送通知给未回复的学生家长）
     *
     * @param notificationId 通知ID
     * @return 发送结果统计
     */
    public Map<String, Object>  remindParentsToReply(Long notificationId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 查询原始通知
        Notification notification = notificationService.selectNotificationById(notificationId);
        if (notification == null) {
            throw new IllegalStateException("未找到通知信息");
        }
        
        // 2. 检查是否超过回复截止时间
        if (notification.getReplyDeadline() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(notification.getReplyDeadline())) {
                result.put("success", false);
                result.put("message", "已超过回复截止时间，无法提示家长回复");
                result.put("remindCount", 0);
                return result;
            }
        }
        
        return remindParentsToReply(notification);
    }

    /**
     * 提示家长回复（根据通知实体）
     *
     * @param notification 通知实体
     * @return 发送结果统计
     */
    public Map<String, Object> remindParentsToReply(Notification notification) {
        // 发送结果统计
        Map<String, Object> result = new HashMap<>();
        // 1. 获取通知ID
        Long notificationId = notification.getNotificationId();

        // 3. 查询发送记录
        NotificationSendRecord sendRecord = notificationSendRecordService.selectByNotificationId(notificationId);
        if (sendRecord == null) {
            throw new IllegalStateException("未找到发送记录");
        }
        
        // 4. 查询未回复的学生列表（按学生分组，只要有一个家长回复就算已回复）
        List<UnrepliedStudentVO> unrepliedStudents = 
            notificationUserReadRecordService.selectUnrepliedStudents(sendRecord.getSendRecordId());

        // 5. 如果没有未回复的学生，则返回成功
        if (unrepliedStudents == null || unrepliedStudents.isEmpty()) {
            result.put("success", true);
            result.put("message", "所有学生家长均已回复");
            result.put("remindCount", 0);
            return result;
        }
        
        log.info("开始发送提醒通知，共 {} 个学生未回复", unrepliedStudents.size());
        
        // 创建提醒记录列表
        int successCount = 0;
        int failCount = 0;
        List<NotificationReminderRecord> reminderRecords = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // 构建提醒消息内容（只需要构建一次）
        String remindContent = buildRemindContent(notification);

        // 6. 为每个未回复的学生发送提醒通知
        for (UnrepliedStudentVO student : unrepliedStudents) {
            String studentUserId = student.getStudentUserId();
            List<String> parentUserIdList = student.getParentUserIds();
            
            if (studentUserId == null || parentUserIdList == null || parentUserIdList.isEmpty()) {
                continue;
            }
            
            // 直接使用 List 的 toString() 方法存储为字符串
            String parentUserIdsStr = parentUserIdList.toString();
            
            try {
                // 分批发送提醒消息
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
                log.error("发送提醒通知失败，学生ID: {}", studentUserId, e);
                failCount++;
                
                // 即使失敗也建立記錄
                reminderRecords.add(buildReminderRecord(
                        notificationId, sendRecord.getSendRecordId(),
                        studentUserId, parentUserIdsStr, now, "2"));
            }
        }
        
        // 7. 批量保存提醒记录
        if (!reminderRecords.isEmpty()) {
            notificationReminderRecordService.batchSave(reminderRecords);
        }
        
        // 8. 构建返回结果
        result.put("success", true);
        result.put("remindCount", unrepliedStudents.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        
        // 根据发送结果生成不同的提示信息
        if (failCount == 0) {
            // 全部成功
            result.put("message", String.format("提醒发送成功，共发送 %d 个学生", successCount));
        } else if (successCount == 0) {
            // 全部失败
            result.put("success", false);
            result.put("message", String.format("微信发送失败，共 %d 个学生未能发送提醒", failCount));
        } else {
            // 部分成功
            result.put("message", String.format("提醒发送完成，成功 %d 个，失败 %d 个（微信发送异常）", successCount, failCount));
        }
        
        return result;
    }
    
    /**
     * 构建提醒消息内容
     *
     * @param notification 通知对象
     * @return 提醒消息内容
     */
    private String buildRemindContent(Notification notification) {
        // 标题
        String title = notification.getTitle() == null ? "" : notification.getTitle().trim();
        // 跳转链接
        String noticeUrl = notification.getJumpUrl();
        
        // 如果通告没有自定义的跳转链接，则使用默认的详情页链接
        if (noticeUrl == null || noticeUrl.trim().isEmpty()) {
            noticeUrl = noticeBaseUrl + notification.getNotificationId();
        }
        
        // 格式化回覆截止時間
        String replyDeadline = notification.getReplyDeadline() != null
                ? notification.getReplyDeadline().format(DATE_FORMATTER) : "";
        
        return "🔔 溫馨提示\n" +
               "───────────────\n" +
               "您有一條通告需要回覆\n" +
               "───────────────\n" +
               "📌 標題：\n" + title + "\n\n" +
               "⏰ 回復截止時間：\n" + replyDeadline + "\n\n" +
               "👉 請點擊以下連接查看詳情：\n" + noticeUrl;
    }
    
    /**
     * 分批发送提醒消息
     *
     * @param parentUserIds 家长用户ID列表
     * @param content       消息内容
     * @return 是否发送成功
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
            
            // 构建企业微信消息payload
            JSONObject payload = buildParentOnlyPayload(currentBatch, content);
            
            try {
                JSONObject result = wechatWorkHttpClient.sendSchoolNotification(payload);
                Integer errcode = result.getInteger("errcode");
                
                if (errcode != null && errcode == 0) {
                    log.info("第 {}/{} 批提醒消息发送成功", i + 1, totalBatches);
                } else {
                    log.error("第 {}/{} 批提醒消息发送失败: code={}, msg={}", 
                             i + 1, totalBatches, errcode, result.getString("errmsg"));
                    return false;
                }
            } catch (Exception e) {
                log.error("第 {}/{} 批提醒消息发送异常", i + 1, totalBatches, e);
                return false;
            }
        }
        
        return true;
    }

    /**
     * 重新发送失败通知（根据通知ID找到发送失败的用户重新发送）
     *
     * @param notificationId 通知ID
     * @param isAutoTask     是否是定时任务自动重发（自动重发会记录失败次数，满3次不再重发）
     * @return 发送结果统计
     */
    public Map<String, Object> resendFailedNotifications(Long notificationId, boolean isAutoTask) {
        Map<String, Object> result = new HashMap<>();

        // 1. 查询原始通知
        Notification notification = notificationService.selectNotificationById(notificationId);
        if (notification == null) {
            throw new IllegalStateException("未找到通知信息");
        }

        // 2. 查询发送记录
        NotificationSendRecord sendRecord = notificationSendRecordService.selectByNotificationId(notificationId);
        if (sendRecord == null) {
            throw new IllegalStateException("未找到发送记录");
        }

        // 3. 查询发送失败的阅读记录
        List<NotificationUserReadRecord> failedRecords =
            notificationUserReadRecordService.selectFailedRecords(sendRecord.getSendRecordId());

        // 4. 如果是自动任务，过滤掉已经达到最大失败次数（放弃重发）的用户
        if (isAutoTask && failedRecords != null) {
            // 如果是自动重发，过滤掉已经达到最大失败次数（放弃重发）的用户
            Set<String> abandonedIds = notificationResendFailRecordService.selectAbandonedUserIds(notificationId);
            // 过滤掉已经放弃重发的用户
            failedRecords = failedRecords.stream()
                    .filter(record -> !abandonedIds.contains(record.getUserId()))
                    .collect(java.util.stream.Collectors.toList());
        }

        // 沒有失敗的記錄則結束
        if (failedRecords == null || failedRecords.isEmpty()) {
            result.put("success", true);
            result.put("message", "没有需要重发的失败记录");
            result.put("resendCount", 0);
            return result;
        }

        log.info("开始重新发送失败通知，共 {} 条失败记录", failedRecords.size());

        // 5. 按用户类型分组失败记录
        List<String> failedParentIds = new ArrayList<>();
        List<String> failedStudentIds = new ArrayList<>();
        for (NotificationUserReadRecord record : failedRecords) {
            if ("2".equals(record.getUserType())) {
                failedParentIds.add(record.getUserId());
            } else if ("1".equals(record.getUserType())) {
                failedStudentIds.add(record.getUserId());
            }
        }

        // 6. 重新发送并更新每条阅读记录的 send_status
        Set<String> overallSuccessUserIds = new HashSet<>();
        // 保存所有失败用户的失败原因
        Map<String, String> allFailedUserReasons = new HashMap<>();

        // 重新發送家長消息
        if (!failedParentIds.isEmpty()) {
            SendResult parentResult = sendInBatches(notification, failedParentIds, Collections.emptyList(), Collections.emptyList());
            updateReadRecords(failedRecords, "2", parentResult.getSuccessUserIds());
            overallSuccessUserIds.addAll(parentResult.getSuccessUserIds());
            if (parentResult.getFailedUserReasons() != null) allFailedUserReasons.putAll(parentResult.getFailedUserReasons());
        }

        // 重新發送學生消息
        if (!failedStudentIds.isEmpty()) {
            SendResult studentResult = sendInBatches(notification, Collections.emptyList(), failedStudentIds, Collections.emptyList());
            updateReadRecords(failedRecords, "1", studentResult.getSuccessUserIds());
            overallSuccessUserIds.addAll(studentResult.getSuccessUserIds());
            if (studentResult.getFailedUserReasons() != null) allFailedUserReasons.putAll(studentResult.getFailedUserReasons());
        }

        // 如果是自动重发， 记录自动重发的失败信息
        if (isAutoTask) {
            // 遍歷失败记录
            for (NotificationUserReadRecord record : failedRecords) {
                // 存在失败
                if (!overallSuccessUserIds.contains(record.getUserId())) {
                    // 创建失败记录
                    NotificationResendFailRecord failRecord = new NotificationResendFailRecord();
                    failRecord.setNotificationId(notificationId);
                    failRecord.setSendRecordId(sendRecord.getSendRecordId());
                    failRecord.setUserId(record.getUserId());
                    failRecord.setUserType(record.getUserType());
                    failRecord.setStudentUserId(record.getStudentUserId());
                    String reason = allFailedUserReasons.getOrDefault(record.getUserId(), "未知原因");
                    failRecord.setFailReason1("自动重发失败");
                    failRecord.setFailMessage1(reason);
                    // 保存或者更新
                    notificationResendFailRecordService.saveOrUpdate(failRecord);
                }
            }
        }

        // 7. 以学生为维度统计成功/失败数（与 createSendRecord 逻辑一致）
        //    - 按 studentUserId 分组失败记录
        //    - 只要该学生下任意一个家长重发成功，就算该学生成功
        // 建立 studentUserId -> 该学生下所有 userId 集合
        Map<String, Set<String>> studentToUsersMap = new HashMap<>();
        // 建立 userId -> 该用户对应的 studentUserId
        for (NotificationUserReadRecord record : failedRecords) {
            String studentId = record.getStudentUserId();
            if (studentId == null || studentId.trim().isEmpty()) {
                // 直接发给学生的情况，studentUserId = userId
                studentId = record.getUserId();
            }
            studentToUsersMap.computeIfAbsent(studentId, k -> new HashSet<>()).add(record.getUserId());
        }

        int successCount = 0;
        int failCount = 0;
        // 遍历所有学生
        for (Map.Entry<String, Set<String>> entry : studentToUsersMap.entrySet()) {
            boolean anySuccess = false;
            // 遍历该学生的所有用户
            for (String uid : entry.getValue()) {
                // 判断该用户是否成功
                if (overallSuccessUserIds.contains(uid)) {
                    anySuccess = true;
                    break;
                }
            }
            if (anySuccess) successCount++; else failCount++;
        }

        // 8. 更新发送记录的统计信息（以学生为维度）
        NotificationSendRecord updateRecord = buildSendRecordUpdate(sendRecord, successCount);
        notificationSendRecordService.update(updateRecord);

        // 9. 构建返回结果
        result.put("resendCount", studentToUsersMap.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);

        // 10. 构建返回结果
        if (failCount == 0) {
            result.put("success", true);
            result.put("message", String.format("重发成功，共 %d 个学生", successCount));
        } else if (successCount == 0) {
            result.put("success", false);
            result.put("message", String.format("重发失败，共 %d 个学生未能发送", failCount));
        } else {
            result.put("success", true);
            result.put("message", String.format("重发完成，成功 %d 个学生，失败 %d 个学生", successCount, failCount));
        }

        return result;
    }

    /**
     * 构建发送记录更新对象（以学生维度，将重发成功数加到现有记录中）
     *
     * @param sendRecord   原发送记录
     * @param successDelta 本次重发成功的学生数
     * @return 待更新的发送记录对象
     */
    private NotificationSendRecord buildSendRecordUpdate(NotificationSendRecord sendRecord, int successDelta) {
        // 计算新的成功/失败数
        int newSuccessCount = (sendRecord.getSuccessCount() != null ? sendRecord.getSuccessCount() : 0) + successDelta;
        int newFailCount = (sendRecord.getFailCount() != null ? sendRecord.getFailCount() : 0) - successDelta;
        if (newFailCount < 0) newFailCount = 0;

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

    /**
     * 帶個性化消息的分批發送通知
     *
     * @param notification   通知實體
     * @param parentUserIds  家長用戶 ID 列表
     * @param studentUserIds 學生用戶 ID 列表
     * @param partyIds       部門 ID 列表
     * @param messageInfos   家長-學生消息信息列表
     * @return 發送結果（成功數、失敗數）
     */
    private SendResult sendInBatchesWithPersonalization(Notification notification, List<String> parentUserIds,
                                                          List<String> studentUserIds, List<String> partyIds,
                                                          List<ParentStudentMessageInfo> messageInfos) {
        // 如果沒有個性化消息，使用原有的發送邏輯
        if (messageInfos == null || messageInfos.isEmpty()) {
            return sendInBatches(notification, parentUserIds, studentUserIds, partyIds);
        }

        // 按家長用戶 ID 分組消息
        Map<String, List<ParentStudentMessageInfo>> parentToMessagesMap = messageInfos.stream()
                .collect(Collectors.groupingBy(ParentStudentMessageInfo::getParentUserId));

        int successCount = 0;
        int failCount = 0;
        Set<String> successUserIds = new HashSet<>();
        Map<String, String> failedUserReasons = new HashMap<>();

        // 為每個家長的每個學生發送獨立的消息
        for (Map.Entry<String, List<ParentStudentMessageInfo>> entry : parentToMessagesMap.entrySet()) {
            String parentUserId = entry.getKey();
            List<ParentStudentMessageInfo> parentMessages = entry.getValue();

            // 為該家長的每個學生發送單獨的消息
            for (ParentStudentMessageInfo msgInfo : parentMessages) {
                try {
                    // 構建個性化消息內容
                    String content = buildContent(notification, msgInfo.getClassName(), msgInfo.getStudentName(), msgInfo.getStudentUserId());

                    // 構建發送 payload（只發送給當前家長）
                    JSONObject payload = buildPersonalizedPayload(parentUserId, content);

                    // 發送消息
                    JSONObject result = wechatWorkHttpClient.sendSchoolNotification(payload);
                    Integer errcode = result.getInteger("errcode");

                    if (errcode != null && errcode == 0) {
                        successCount++;
                        successUserIds.add(parentUserId);
                        log.debug("成功發送通知給家長 {}，學生 {}", parentUserId, msgInfo.getStudentUserId());
                    } else {
                        failCount++;
                        String reason = "接口返回错误: " + errcode;
                        failedUserReasons.put(parentUserId, reason);
                        log.error("發送通知給家長 {} 失敗: code={}, msg={}", parentUserId, errcode, result.getString("errmsg"));
                    }
                } catch (Exception e) {
                    failCount++;
                    failedUserReasons.put(parentUserId, "发送异常: " + e.getMessage());
                    log.error("發送通知給家長 {} 異常", parentUserId, e);
                }
            }
        }

        log.info("通知 {} 已全部發送完成，成功: {}, 失敗: {}",
                notification.getNotificationId(), successCount, failCount);

        return new SendResult(successCount, failCount, successUserIds, failedUserReasons);
    }

    /**
     * 構建個性化消息的發送 payload（單個家長）
     *
     * @param parentUserId 家長用戶 ID
     * @param content      消息內容
     * @return 企業微信發送 payload
     */
    private JSONObject buildPersonalizedPayload(String parentUserId, String content) {
        return buildParentOnlyPayload(Collections.singletonList(parentUserId), content);
    }

    /**
     * 構建只發送給家長的 payload（支持批量）
     *
     * @param parentUserIds 家長用戶 ID 列表
     * @param content       消息內容
     * @return 企業微信發送 payload
     */
    private JSONObject buildParentOnlyPayload(List<String> parentUserIds, String content) {
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

    /**
     * 構建提醒記錄（統一工廠方法，避免重複代碼）
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

    // =========================================================
    // 定時任務主方法（每個定時任務只調用下方對應的一個方法）
    // =========================================================

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
                    + noticeBaseUrl + "handbook";

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
        
        // 遍歷通知
        for (Notification notification : notificationList) {
            log.info("發現需要提示回復的通知: 標題={}, ID={}",
                    notification.getTitle(), notification.getNotificationId());
            try {
                // 提醒家長
                Map<String, Object> result = remindParentsToReply(notification);
                if (result != null && Boolean.FALSE.equals(result.get("success"))) {
                    failCount++;
                    if (errorMsg.length() < 1000) {
                        errorMsg.append("通知ID ").append(notification.getNotificationId()).append(" 失敗: ").append(result.get("message")).append("; ");
                    }
                } else {
                    remindCount++;
                }
            } catch (Exception e) {
                failCount++;
                if (errorMsg.length() < 1000) {
                    errorMsg.append("通知ID ").append(notification.getNotificationId()).append(" 失敗: ").append(e.getMessage()).append("; ");
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
        List<NotificationSendRecord> failedSendRecords =
                notificationSendRecordService.selectAllFailedRecords();

        if (failedSendRecords == null || failedSendRecords.isEmpty()) {
            log.info("沒有發送失敗的通知記錄，任務結束");
            return TaskResult.success(0, 0, "無失敗通知需重發");
        }

        log.info("共有 {} 條發送失敗的通知記錄，開始逐一重發", failedSendRecords.size());

        int successNotifications = 0;
        int failNotifications = 0;

        // 遍歷記錄
        for (NotificationSendRecord sendRecord : failedSendRecords) {
            Long notificationId = sendRecord.getNotificationId();
            try {
                log.info("重發失敗通知: notificationId={}", notificationId);
                // 重發失敗通知
                Map<String, Object> result = resendFailedNotifications(notificationId, true);
                // 如果返回結果包含失敗，或者是部分成功，則算作失敗
                if (result != null && (Boolean.FALSE.equals(result.get("success")) || (Integer) result.getOrDefault("failCount", 0) > 0)) {
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
}
