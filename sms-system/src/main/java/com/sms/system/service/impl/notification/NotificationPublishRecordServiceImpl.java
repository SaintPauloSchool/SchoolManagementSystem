package com.sms.system.service.impl.notification;

import com.sms.system.entity.SysSchoolFamilyContact;
import com.sms.system.entity.notification.Notification;
import com.sms.system.entity.notification.NotificationReminderRecord;
import com.sms.system.entity.notification.NotificationResendFailRecord;
import com.sms.system.entity.notification.NotificationSendRecord;
import com.sms.system.entity.notification.NotificationUserReadRecord;
import com.sms.system.entity.notification.SendResult;
import com.sms.system.service.notification.INotificationPublishRecordService;
import com.sms.system.service.notification.INotificationReminderRecordService;
import com.sms.system.service.notification.INotificationResendFailRecordService;
import com.sms.system.service.notification.INotificationSendRecordService;
import com.sms.system.service.notification.INotificationUserReadRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通知發佈相關持久化 Service 實現。
 * <p>
 * 由 {@code NotificationPublishHandler} 在企業微信發送完成後調用，負責將發送結果寫入數據庫。
 * 本類<strong>不再查庫解析接收人</strong>，所有接收人信息均由上游 {@code resolveReceivers} 解析好後直接傳入。
 * </p>
 * <p>主要寫入兩張表：</p>
 * <ul>
 *   <li>{@code notification_send_record}：本次發送的匯總統計（總數/成功/失敗/狀態）</li>
 *   <li>{@code notification_user_read_record}：每位接收人的閱讀/回覆/發送狀態明細</li>
 * </ul>
 * <p>閱讀記錄寫入分三條路徑，對應不同接收人來源：</p>
 * <ul>
 *   <li>企微家校 → {@code bindings}（家長+學生+班級三方綁定）</li>
 *   <li>自定義家校 → {@code parentStudentUserIds}（家長 userid → 學生 userid，來自成員表解析結果）</li>
 *   <li>直接選學生 → {@code studentUserIds}（當前業務較少使用）</li>
 * </ul>
 */
@Service
public class NotificationPublishRecordServiceImpl implements INotificationPublishRecordService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublishRecordServiceImpl.class);

    /** 發送狀態：全部成功 */
    private static final String SEND_STATUS_SUCCESS = "2";
    /** 發送狀態：全部失敗 */
    private static final String SEND_STATUS_FAIL = "3";
    /** 發送狀態：部分成功 */
    private static final String SEND_STATUS_PARTIAL = "4";

    /** user_type=1：學生 */
    private static final String USER_TYPE_STUDENT = "1";
    /** user_type=2：家長 */
    private static final String USER_TYPE_PARENT = "2";

    @Autowired
    private INotificationSendRecordService notificationSendRecordService;

    @Autowired
    private INotificationUserReadRecordService notificationUserReadRecordService;

    @Autowired
    private INotificationReminderRecordService notificationReminderRecordService;

    @Autowired
    private INotificationResendFailRecordService notificationResendFailRecordService;

    // -------------------------------------------------------------------------
    // 發佈落庫
    // -------------------------------------------------------------------------

    /**
     * 保存發佈結果：發送記錄 + 用戶閱讀記錄（同一事務）。
     * <p>
     * 調用時機：企微家校通知 API 調用完成、已取得 {@link SendResult} 之後。
     * 所有參數均為解析階段的內存快照，寫庫時不再查詢成員表或綁定表。
     * </p>
     *
     * @param studentUserIds        直接作為接收目標的學生 userid 列表（當前業務多為空）
     * @param sendResult            企微發送結果，含成功/失敗家長 userid 集合
     * @param parentUserIds         實際發送的家長 userid 列表
     * @param studentDepartmentIds  家長 userid → 自定義部門 ID（用於閱讀記錄 department_id）
     * @param parentStudentUserIds  家長 userid → 學生 userid（僅自定義家校使用；
     *                              解析階段已從 {@code sys_school_department_member.student_user_id} 讀好，此處直接使用）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePublishRecords(Notification notification,
                                   List<String> studentUserIds,
                                   SendResult sendResult,
                                   List<SysSchoolFamilyContact> relations,
                                   List<String> parentUserIds,
                                   Map<String, Long> studentDepartmentIds,
                                   Map<String, String> parentStudentUserIds) {
        NotificationSendRecord sendRecord = buildSendRecord(
                notification, studentUserIds, parentUserIds, sendResult, relations);
        notificationSendRecordService.save(sendRecord);

        List<NotificationUserReadRecord> readRecords = buildUserReadRecords(
                sendRecord.getSendRecordId(), parentUserIds, studentUserIds,
                sendResult.getSuccessUserIds(), relations, studentDepartmentIds, parentStudentUserIds);
        notificationUserReadRecordService.batchSave(readRecords);
    }

    // -------------------------------------------------------------------------
    // 提醒 / 重發落庫
    // -------------------------------------------------------------------------

    /** 批量保存提醒記錄（提醒消息發送成功後調用） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveReminderRecords(List<NotificationReminderRecord> reminderRecords) {
        if (reminderRecords == null || reminderRecords.isEmpty()) {
            return;
        }
        notificationReminderRecordService.batchSave(reminderRecords);
    }

    /**
     * 保存重發結果：更新閱讀記錄發送狀態、記錄自動重發失敗、更新發送匯總統計（同一事務）。
     *
     * @param failedRecords         本次重發涉及的失敗閱讀記錄（從庫中查出，自帶 student_user_id）
     * @param overallSuccessUserIds 重發後成功的用戶 userid 集合
     * @param isAutoTask            true=定時任務自動重發，會累計失敗次數到 resend_fail_record
     * @param studentSuccessCount   以學生為維度的成功增量，用於遞增 send_record.success_count
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveResendRecords(Long notificationId,
                                  NotificationSendRecord sendRecord,
                                  List<NotificationUserReadRecord> failedRecords,
                                  Set<String> overallSuccessUserIds,
                                  Map<String, String> failedUserReasons,
                                  boolean isAutoTask,
                                  int studentSuccessCount) {
        updateReadRecordsAfterResend(failedRecords, overallSuccessUserIds);

        if (isAutoTask) {
            saveAutoResendFailRecords(notificationId, sendRecord, failedRecords,
                    overallSuccessUserIds, failedUserReasons);
        }

        NotificationSendRecord updateRecord = buildSendRecordUpdate(sendRecord, studentSuccessCount);
        notificationSendRecordService.update(updateRecord);
    }

    // -------------------------------------------------------------------------
    // 發送記錄構建
    // -------------------------------------------------------------------------

    /**
     * 根據發送結果構建 {@code notification_send_record} 匯總行。
     * <p>統計維度說明：</p>
     * <ul>
     *   <li>有 bindings 或 studentUserIds 時：以<strong>學生</strong>為維度統計；
     *       學生本人或任一關聯家長發送成功，該學生計為成功</li>
     *   <li>僅有 parentUserIds（典型：純自定義家校批量發送）時：以<strong>家長</strong>為維度統計</li>
     * </ul>
     */
    private NotificationSendRecord buildSendRecord(Notification notification,
                                                     List<String> studentUserIds,
                                                     List<String> parentUserIds,
                                                     SendResult sendResult,
                                                     List<SysSchoolFamilyContact> relations) {
        NotificationSendRecord sendRecord = new NotificationSendRecord();
        sendRecord.setNotificationId(notification.getNotificationId());
        sendRecord.setSenderId(notification.getSenderId());
        sendRecord.setSenderName(notification.getSenderName());
        sendRecord.setSendTime(LocalDateTime.now());

        int initialCapacity = relations == null ? 16 : (int) (relations.size() / 0.75f) + 1;
        Map<String, Set<String>> studentParentMap = new HashMap<>(initialCapacity);
        if (relations != null) {
            for (SysSchoolFamilyContact relation : relations) {
                String studentId = relation.getStudentUserId();
                String parentId = relation.getParentUserId();
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

        int totalCount;
        int successCount = 0;
        int failCount = 0;
        Set<String> successUserIds = sendResult.getSuccessUserIds() != null
                ? sendResult.getSuccessUserIds() : Collections.emptySet();

        if (allTargetStudents.isEmpty() && parentUserIds != null && !parentUserIds.isEmpty()) {
            // 自定義家校等無 binding 場景：按家長人數統計
            totalCount = parentUserIds.size();
            for (String parentId : parentUserIds) {
                if (successUserIds.contains(parentId)) {
                    successCount++;
                } else {
                    failCount++;
                }
            }
        } else {
            // 企微家校：按學生人數統計，家長發送成功即算該學生成功
            totalCount = allTargetStudents.size();
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

    // -------------------------------------------------------------------------
    // 閱讀記錄構建（三條寫入路徑）
    // -------------------------------------------------------------------------

    /**
     * 構建本次發送對應的全部閱讀記錄。
     * <p>按順序執行三個 append 方法，後續路徑會跳過已寫入的家長，避免重複：</p>
     * <ol>
     *   <li>{@link #appendParentReadRecords} — 未被 binding 覆蓋的家長（自定義家校走此路徑）</li>
     *   <li>{@link #appendStudentReadRecords} — 直接發給學生的記錄</li>
     * </ol>
     */
    private List<NotificationUserReadRecord> buildUserReadRecords(Long sendRecordId,
                                                                  List<String> parentUserIds,
                                                                  List<String> studentUserIds,
                                                                  Set<String> successUserIds,
                                                                  List<SysSchoolFamilyContact> relations,
                                                                  Map<String, Long> studentDepartmentIds,
                                                                  Map<String, String> parentStudentUserIds) {
        List<NotificationUserReadRecord> readRecords = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        // 已寫入閱讀記錄的家長 userid，供後續路徑去重
        Set<String> recordedParentIds = new HashSet<>();

        appendRelationReadRecords(readRecords, sendRecordId, relations, successUserIds, now, recordedParentIds);
        appendParentReadRecords(readRecords, sendRecordId, parentUserIds, successUserIds,
                studentDepartmentIds, parentStudentUserIds, now, recordedParentIds);
        appendStudentReadRecords(readRecords, sendRecordId, studentUserIds, successUserIds,
                studentDepartmentIds, now);
        return readRecords;
    }

    /**
     * 路徑一：企微家校 — 按 binding 寫家長閱讀記錄。
     * <p>
     * 每條 binding 包含 parent_user_id、student_user_id、department_id，
     * 直接寫入閱讀記錄，無需再查庫。
     * user_type 固定為家長（2）。
     * </p>
     */
    private void appendRelationReadRecords(List<NotificationUserReadRecord> readRecords, Long sendRecordId,
                                          List<SysSchoolFamilyContact> relations, Set<String> successUserIds,
                                          LocalDateTime now, Set<String> recordedParentIds) {
        if (relations == null) {
            return;
        }
        Set<String> processedRelationKeys = new HashSet<>();
        for (SysSchoolFamilyContact relation : relations) {
            String parentUserId = relation.getParentUserId();
            if (parentUserId == null) {
                continue;
            }
            String key = parentUserId + "_" + relation.getStudentUserId() + "_"
                    + (relation.getDepartmentId() != null ? relation.getDepartmentId() : "null");
            if (!processedRelationKeys.add(key)) {
                log.debug("buildUserReadRecords: 跳過重複的關係: parentUserId={}, studentUserId={}, departmentId={}",
                        parentUserId, relation.getStudentUserId(), relation.getDepartmentId());
                continue;
            }
            readRecords.add(createReadRecord(sendRecordId, parentUserId, USER_TYPE_PARENT,
                    relation.getStudentUserId(), relation.getDepartmentId(),
                    successUserIds.contains(parentUserId), now));
            recordedParentIds.add(parentUserId);
        }
    }

    /**
     * 路徑二：自定義家校等無 binding 的家長 — 補充寫家長閱讀記錄。
     * <p>
     * 跳過已在 binding 路徑寫過的家長（{@code recordedParentIds}）。
     * student_user_id 直接從 {@code parentStudentUserIds} 取，不再查成員表或關係表；
     * department_id 從 {@code studentDepartmentIds} 取。
     * </p>
     * <p>
     * {@code parentStudentUserIds} 的數據在 {@code resolveReceivers} 階段
     * 已從 {@code sys_school_department_member.student_user_id} 解析好，
     * 對應本次通知實際選中的成員，避免一家长多學生時猜錯。
     * </p>
     */
    private void appendParentReadRecords(List<NotificationUserReadRecord> readRecords, Long sendRecordId,
                                         List<String> parentUserIds, Set<String> successUserIds,
                                         Map<String, Long> studentDepartmentIds,
                                         Map<String, String> parentStudentUserIds,
                                         LocalDateTime now, Set<String> recordedParentIds) {
        if (parentUserIds == null) {
            return;
        }
        for (String userId : parentUserIds) {
            if (recordedParentIds.contains(userId)) {
                continue;
            }
            Long departmentId = studentDepartmentIds != null ? studentDepartmentIds.get(userId) : null;
            String studentUserId = parentStudentUserIds != null ? parentStudentUserIds.get(userId) : null;
            readRecords.add(createReadRecord(sendRecordId, userId, USER_TYPE_PARENT, studentUserId,
                    departmentId, successUserIds.contains(userId), now));
        }
    }

    /**
     * 路徑三：直接以學生為接收目標時寫學生閱讀記錄。
     * <p>user_type 為學生（1），student_user_id 即 user_id 本身。當前業務較少使用。</p>
     */
    private void appendStudentReadRecords(List<NotificationUserReadRecord> readRecords, Long sendRecordId,
                                          List<String> studentUserIds, Set<String> successUserIds,
                                          Map<String, Long> studentDepartmentIds, LocalDateTime now) {
        if (studentUserIds == null) {
            return;
        }
        for (String userId : studentUserIds) {
            Long departmentId = studentDepartmentIds != null ? studentDepartmentIds.get(userId) : null;
            readRecords.add(createReadRecord(sendRecordId, userId, USER_TYPE_STUDENT, userId, departmentId,
                    successUserIds.contains(userId), now));
        }
    }

    /**
     * 創建單條 {@code notification_user_read_record}。
     *
     * @param userId         接收人企微 userid（家長或學生）
     * @param userType       1=學生，2=家長
     * @param studentUserId  關聯學生 userid（供統計頁 JOIN sys_school_family_contact；家長記錄必填）
     * @param departmentId   關聯部門/班級 ID
     * @param sendSuccess    本次企微發送是否成功
     */
    private NotificationUserReadRecord createReadRecord(Long sendRecordId, String userId, String userType,
                                                        String studentUserId, Long departmentId,
                                                        boolean sendSuccess, LocalDateTime createTime) {
        NotificationUserReadRecord record = new NotificationUserReadRecord();
        record.setSendRecordId(sendRecordId);
        record.setUserId(userId);
        record.setUserType(userType);
        record.setIsRead("0");
        record.setReplyStatus("0");
        record.setSendStatus(sendSuccess ? "1" : "0");
        record.setStudentUserId(studentUserId);
        record.setDepartmentId(departmentId);
        record.setCreateTime(createTime);
        return record;
    }

    // -------------------------------------------------------------------------
    // 重發相關
    // -------------------------------------------------------------------------

    /**
     * 重發後按 userid 更新閱讀記錄的 send_status（1=成功，0=失敗）。
     * <p>閱讀記錄在首次發佈時已寫好 student_user_id，重發無需再傳映射。</p>
     */
    private void updateReadRecordsAfterResend(List<NotificationUserReadRecord> records,
                                              Set<String> successUserIds) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (NotificationUserReadRecord record : records) {
            String newStatus = successUserIds.contains(record.getUserId()) ? "1" : "0";
            notificationUserReadRecordService.updateSendStatus(record.getReadId(), newStatus);
        }
    }

    /**
     * 自動重發仍失敗時，寫入/累計 {@code notification_resend_fail_record}。
     * <p>達失敗次數上限後定時任務不再重發該用戶。</p>
     */
    private void saveAutoResendFailRecords(Long notificationId,
                                           NotificationSendRecord sendRecord,
                                           List<NotificationUserReadRecord> failedRecords,
                                           Set<String> overallSuccessUserIds,
                                           Map<String, String> failedUserReasons) {
        for (NotificationUserReadRecord record : failedRecords) {
            if (!overallSuccessUserIds.contains(record.getUserId())) {
                NotificationResendFailRecord failRecord = new NotificationResendFailRecord();
                failRecord.setNotificationId(notificationId);
                failRecord.setSendRecordId(sendRecord.getSendRecordId());
                failRecord.setUserId(record.getUserId());
                failRecord.setUserType(record.getUserType());
                failRecord.setStudentUserId(record.getStudentUserId());
                String reason = failedUserReasons.getOrDefault(record.getUserId(), "未知原因");
                failRecord.setFailReason1("自動重發失敗");
                failRecord.setFailMessage1(reason);
                notificationResendFailRecordService.saveOrUpdate(failRecord);
            }
        }
    }

    /**
     * 重發成功後更新 send_record 匯總：success_count 遞增、fail_count 遞減，並重算 send_status。
     *
     * @param successDelta 本次以學生為維度新增的成功數
     */
    private NotificationSendRecord buildSendRecordUpdate(NotificationSendRecord sendRecord, int successDelta) {
        int newSuccessCount = (sendRecord.getSuccessCount() != null ? sendRecord.getSuccessCount() : 0) + successDelta;
        int newFailCount = (sendRecord.getFailCount() != null ? sendRecord.getFailCount() : 0) - successDelta;
        if (newFailCount < 0) {
            newFailCount = 0;
        }

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
}
