package com.sms.handler.notification;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.framework.wechat.WechatWorkHttpClient;
import com.sms.system.entity.notification.Notification;
import com.sms.system.entity.notification.NotificationCc;
import com.sms.system.entity.notification.NotificationReceiver;
import com.sms.system.entity.notification.NotificationSendRecord;
import com.sms.system.entity.notification.NotificationUserReadRecord;
import com.sms.system.entity.vo.ResolvedReceiversVO;
import com.sms.system.entity.notification.SendResult;
import com.sms.system.service.notification.INotificationCcService;
import com.sms.system.service.notification.INotificationReceiverService;
import com.sms.system.service.notification.INotificationSendRecordService;
import com.sms.system.service.notification.INotificationUserReadRecordService;
import com.sms.system.mapper.SysDepartmentParentBindingMapper;
import com.sms.system.entity.SysDepartmentParentBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 通告發佈處理器
 * 負責將通告消息推送給指定的接收者（如通過企業微信發送家校通知）
 */
@Component
public class NotificationPublishHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublishHandler.class);

    /**
     * 默認的通告查看基礎 URL。如果通告本身沒有跳轉鏈接，將使用此基礎 URL 拼接通告 ID。
     */
    @Value("${wechat.work.noticeBaseUrl:http://10.32.96.55:8080/notice/}")
    private String noticeBaseUrl;

    @Value("${wechat.work.agentId:#{null}}")
    private Integer agentId;

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
    private SysDepartmentParentBindingMapper sysDepartmentParentBindingMapper;

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

        List<String> parentUserIds = resolvedReceivers.getParentUserIds() == null ? Collections.emptyList() : resolvedReceivers.getParentUserIds();
        List<String> studentUserIds = resolvedReceivers.getStudentUserIds() == null ? Collections.emptyList() : resolvedReceivers.getStudentUserIds();
        List<String> partyIds = resolvedReceivers.getPartyIds() == null ? Collections.emptyList() : resolvedReceivers.getPartyIds();
        List<SysDepartmentParentBinding> bindings = resolvedReceivers.getBindings() == null ? Collections.emptyList() : resolvedReceivers.getBindings();

        // 如果沒有任何接收者，則拋出異常避免無效調用
        if (parentUserIds.isEmpty() && studentUserIds.isEmpty() && partyIds.isEmpty()) {
            log.warn("未找到有效的微信收件人進行通知 {}", notification.getNotificationId());
            throw new IllegalStateException("未解析出有效的微信接收者");
        }

        // 2. 分批發送通知，並獲取發送結果
        SendResult sendResult = sendInBatches(notification, parentUserIds, studentUserIds, partyIds);

        // 3. 創建發送記錄（包含成功/失敗統計）
        NotificationSendRecord sendRecord = createSendRecord(notification, studentUserIds, sendResult, bindings);
        notificationSendRecordService.save(sendRecord);

        // 4. 創建用戶閱讀記錄（帶入每個用戶的發送成功狀態）
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
        payload.put("duplicate_check_interval", 1800); // 防重覆發送校驗時間，默認1800秒
        
        return payload;
    }

    /**
     * 構建消息文本內容
     *
     * @param notification 通告實體
     * @return 格式化後的文本內容
     */
    private String buildContent(Notification notification) {
        // 標題
        String title = notification.getTitle() == null ? "" : notification.getTitle().trim();
        // 設置跳轉地址
        String noticeUrl = notification.getJumpUrl();
        
        // 如果通告沒有自定義的跳轉鏈接，則使用默認的詳情頁鏈接
        if (noticeUrl == null || noticeUrl.trim().isEmpty()) {
            noticeUrl = noticeBaseUrl + notification.getNotificationId();
        }
        
        // 格式化發佈時間
        String publishTime = formatPublishTime(notification.getCreateTime());
        
        return "📢 您有一條新的通告\n"
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
        int parentBatchSize = 1000;
        int studentBatchSize = 1000;
        int partyBatchSize = 100;

        // 計算需要的批次數量
        int parentBatches = (int) Math.ceil((double) parentUserIds.size() / parentBatchSize);
        int studentBatches = (int) Math.ceil((double) studentUserIds.size() / studentBatchSize);
        int partyBatches = (int) Math.ceil((double) partyIds.size() / partyBatchSize);
        
        int totalBatches = Math.max(Math.max(parentBatches, studentBatches), partyBatches);
        
        log.info("通知 {} 需要分 {} 批發送（家長 {} 批，學生 {} 批，部門 {} 批）",
                notification.getNotificationId(), totalBatches, parentBatches, studentBatches, partyBatches);

        int successCount = 0;
        int failCount = 0;
        // 用於記錄成功接收到通知的用戶 ID（家長 + 學生）
        Set<String> successUserIds = new HashSet<>();

        // 分批發送
        for (int i = 0; i < totalBatches; i++) {
            // 截取當前批次的數據
            List<String> currentParentIds = extractBatch(parentUserIds, i, parentBatchSize);
            List<String> currentStudentIds = extractBatch(studentUserIds, i, studentBatchSize);
            List<String> currentPartyIds = extractBatch(partyIds, i, partyBatchSize);
        
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
                log.error("通知 {} 第 {} 批發送失敗: code={}, msg={}",
                        notification.getNotificationId(), i + 1, errcode, result.getString("errmsg"));
                failCount += currentParentIds.size() + currentStudentIds.size();
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
        
        return new SendResult(successCount, failCount, successUserIds);
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
    private String formatPublishTime(Date createTime) {
        if (createTime == null) {
            return "未知";
        }
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(createTime);
    }
    
    /**
     * 分批發送抄送消息
     *
     * @param notification 通知實體
     * @param userIds      接收者 userid 列表
     */
    private void sendCcInBatches(Notification notification, List<String> userIds) {
        int batchSize = 1000; // 每批最多 1000 個用戶
        
        // 計算需要的批次數量
        int totalBatches = (int) Math.ceil((double) userIds.size() / batchSize);
        
        log.info("通知 {} 的抄送消息需要分 {} 批發送，共 {} 個接收者",
                notification.getNotificationId(), totalBatches, userIds.size());
        
        // 分批發送
        for (int i = 0; i < totalBatches; i++) {
            // 截取當前批次的數據
            List<String> currentUserIds = extractBatch(userIds, i, batchSize);
            
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
        // 跳轉鏈接
        String noticeUrl = notification.getJumpUrl();
        if (noticeUrl == null || noticeUrl.trim().isEmpty()) {
            noticeUrl = noticeBaseUrl + notification.getNotificationId();
        }
        textcard.put("url", noticeUrl);
        
        // 按鈕文字
        textcard.put("btntxt", "查看詳情");
        
        payload.put("textcard", textcard);
        
        // 其他發送配置
        payload.put("enable_id_trans", 0);
        payload.put("enable_duplicate_check", 0);
        payload.put("duplicate_check_interval", 1800); // 防重覆發送校驗時間，默認1800秒
        
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
        sendRecord.setSendTime(new Date());
        
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
            sendRecord.setSendStatus("2"); // 2-發送成功
        } else if (successCount == 0 && totalCount > 0) {
            sendRecord.setSendStatus("3"); // 3-發送失敗
        } else {
            sendRecord.setSendStatus("4"); // 4-部分成功
        }
        
        sendRecord.setCreateTime(new Date());
        
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
        Date now = new Date();
        
        // 建立 parentUserId -> 任何一個對應的 studentUserId 的映射
        int initialCapacity = bindings == null ? 16 : (int) (bindings.size() / 0.75f) + 1;
        Map<String, String> parentToStudentMap = new HashMap<>(initialCapacity);
        if (bindings != null) {
            for (SysDepartmentParentBinding binding : bindings) {
                if (binding.getParentUserId() != null && binding.getStudentUserId() != null) {
                    parentToStudentMap.put(binding.getParentUserId(), binding.getStudentUserId());
                }
            }
        }
        
        // 為每個家長創建閱讀記錄（user_type = 2）
        if (parentUserIds != null) {
            for (String userId : parentUserIds) {
                NotificationUserReadRecord record = new NotificationUserReadRecord();
                record.setSendRecordId(sendRecordId);
                record.setUserId(userId);
                record.setUserType("2"); // 2-家長
                record.setIsRead("0"); // 0-未讀
                record.setReplyStatus("0"); // 0-未回覆
                // 根據發送結果設置 send_status（1-成功，0-失敗）
                record.setSendStatus(successUserIds.contains(userId) ? "1" : "0");
                // 設置關聯的 studentUserId
                record.setStudentUserId(parentToStudentMap.get(userId));
                record.setCreateTime(now);
                readRecords.add(record);
            }
        }

        // 為每個學生創建閱讀記錄（user_type = 1）
        if (studentUserIds != null) {
            for (String userId : studentUserIds) {
                NotificationUserReadRecord record = new NotificationUserReadRecord();
                record.setSendRecordId(sendRecordId);
                record.setUserId(userId);
                record.setUserType("1"); // 1-學生
                record.setIsRead("0"); // 0-未讀
                record.setReplyStatus("0"); // 0-未回覆
                // 根據發送結果設置 send_status（1-成功，0-失敗）
                record.setSendStatus(successUserIds.contains(userId) ? "1" : "0");
                // 學生本身的 studentUserId 就是自己
                record.setStudentUserId(userId);
                record.setCreateTime(now);
                readRecords.add(record);
            }
        }

        return readRecords;
    }
}
