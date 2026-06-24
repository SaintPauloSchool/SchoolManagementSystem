package com.sms.system.service.impl.notification;

import com.sms.system.entity.SysDepartmentParentBinding;
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
import java.util.stream.Collectors;

/**
 * 通知發佈相關持久化 Service 實現
 * <p>
 * 由 NotificationPublishHandler 在企業微信發送完成後調用，
 * 負責將發送結果、閱讀記錄、提醒記錄、重發結果等寫入數據庫。
 * </p>
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

    @Autowired
    private INotificationSendRecordService notificationSendRecordService;

    @Autowired
    private INotificationUserReadRecordService notificationUserReadRecordService;

    @Autowired
    private INotificationReminderRecordService notificationReminderRecordService;

    @Autowired
    private INotificationResendFailRecordService notificationResendFailRecordService;

    /**
     * 保存發佈結果：發送記錄 + 用戶閱讀記錄（同一事務）
     *
     * @param notification          通知實體
     * @param studentUserIds        學生用戶 ID 列表
     * @param sendResult            企業微信發送結果
     * @param bindings              家長-學生綁定關係
     * @param parentUserIds         家長用戶 ID 列表
     * @param studentDepartmentIds  學生對應部門 ID 映射
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePublishRecords(Notification notification,
                                   List<String> studentUserIds,
                                   SendResult sendResult,
                                   List<SysDepartmentParentBinding> bindings,
                                   List<String> parentUserIds,
                                   Map<String, Long> studentDepartmentIds) {
        // 根據發送結果構建發送記錄
        NotificationSendRecord sendRecord = buildSendRecord(notification, studentUserIds, sendResult, bindings);
        notificationSendRecordService.save(sendRecord);

        // 構建用戶閱讀記錄列表
        List<NotificationUserReadRecord> readRecords = buildUserReadRecords(
                sendRecord.getSendRecordId(), parentUserIds, studentUserIds,
                sendResult.getSuccessUserIds(), bindings, studentDepartmentIds);
        notificationUserReadRecordService.batchSave(readRecords);
    }

    /**
     * 批量保存提醒記錄
     *
     * @param reminderRecords 提醒記錄列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveReminderRecords(List<NotificationReminderRecord> reminderRecords) {
        if (reminderRecords == null || reminderRecords.isEmpty()) {
            return;
        }
        notificationReminderRecordService.batchSave(reminderRecords);
    }

    /**
     * 保存重發結果：更新閱讀記錄、自動重發失敗記錄、發送統計（同一事務）
     *
     * @param notificationId        通知 ID
     * @param sendRecord            原發送記錄
     * @param failedRecords         本次重發涉及的失敗閱讀記錄
     * @param overallSuccessUserIds 重發成功的用戶 ID 集合
     * @param failedUserReasons     重發失敗用戶及原因映射
     * @param isAutoTask            是否為定時任務自動重發（自動重發會累計失敗次數）
     * @param studentSuccessCount   以學生為維度的重發成功數，用於更新發送記錄統計
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

    /**
     * 根據發送結果構建發送記錄
     * <p>
     * 以學生為維度統計成功/失敗數：學生本人或任一關聯家長發送成功即計為該學生成功。
     * </p>
     */
    private NotificationSendRecord buildSendRecord(Notification notification,
                                                     List<String> studentUserIds,
                                                     SendResult sendResult,
                                                     List<SysDepartmentParentBinding> bindings) {
        NotificationSendRecord sendRecord = new NotificationSendRecord();
        sendRecord.setNotificationId(notification.getNotificationId());
        sendRecord.setSenderId(notification.getSenderId());
        sendRecord.setSenderName(notification.getSenderName());
        sendRecord.setSendTime(LocalDateTime.now());

        // 建立學生 -> 家長集合映射
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
     * 構建用戶閱讀記錄列表
     * <p>
     * 優先按綁定關係生成家長記錄，再補充未覆蓋的家長與學生記錄，並標記各用戶的發送成功狀態。
     * </p>
     */
    private List<NotificationUserReadRecord> buildUserReadRecords(Long sendRecordId,
                                                                  List<String> parentUserIds,
                                                                  List<String> studentUserIds,
                                                                  Set<String> successUserIds,
                                                                  List<SysDepartmentParentBinding> bindings,
                                                                  Map<String, Long> studentDepartmentIds) {
        List<NotificationUserReadRecord> readRecords = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Set<String> processedBindingKeys = new HashSet<>();

        // 按綁定關係生成家長閱讀記錄
        if (bindings != null) {
            for (SysDepartmentParentBinding binding : bindings) {
                String parentUserId = binding.getParentUserId();
                if (parentUserId == null) {
                    continue;
                }
                String key = parentUserId + "_" + binding.getStudentUserId() + "_"
                        + (binding.getDepartmentId() != null ? binding.getDepartmentId() : "null");
                if (processedBindingKeys.contains(key)) {
                    log.debug("buildUserReadRecords: 跳過重複的綁定關係: parentUserId={}, studentUserId={}, departmentId={}",
                            parentUserId, binding.getStudentUserId(), binding.getDepartmentId());
                    continue;
                }
                processedBindingKeys.add(key);
                boolean sendSuccess = successUserIds.contains(parentUserId);
                readRecords.add(createReadRecord(sendRecordId, parentUserId, "2", binding.getStudentUserId(),
                        binding.getDepartmentId(), sendSuccess, now));
            }
        }

        // 補充未通過綁定關係覆蓋的家長
        Set<String> parentsWithRecords = readRecords.stream()
                .filter(r -> "2".equals(r.getUserType()))
                .map(NotificationUserReadRecord::getUserId)
                .collect(Collectors.toSet());

        if (parentUserIds != null) {
            for (String userId : parentUserIds) {
                if (!parentsWithRecords.contains(userId)) {
                    boolean sendSuccess = successUserIds.contains(userId);
                    readRecords.add(createReadRecord(sendRecordId, userId, "2", null, null, sendSuccess, now));
                }
            }
        }

        // 生成學生閱讀記錄
        if (studentUserIds != null) {
            for (String userId : studentUserIds) {
                boolean sendSuccess = successUserIds.contains(userId);
                Long departmentId = studentDepartmentIds != null ? studentDepartmentIds.get(userId) : null;
                readRecords.add(createReadRecord(sendRecordId, userId, "1", userId, departmentId, sendSuccess, now));
            }
        }
        return readRecords;
    }

    /**
     * 創建單條閱讀記錄
     *
     * @param sendRecordId   發送記錄 ID
     * @param userId         用戶 ID（家長或學生）
     * @param userType       用戶類型（1=學生，2=家長）
     * @param studentUserId  關聯學生 ID
     * @param departmentId   部門 ID
     * @param sendSuccess    是否發送成功
     * @param createTime     創建時間
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

    /**
     * 重發後批量更新閱讀記錄的發送狀態
     *
     * @param records         待更新的閱讀記錄
     * @param successUserIds  重發成功的用戶 ID 集合
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
     * 記錄自動重發仍失敗的用戶（累計失敗次數，達上限後不再重發）
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
     * 構建發送記錄更新對象（重發成功後遞增成功數、遞減失敗數）
     *
     * @param sendRecord   原發送記錄
     * @param successDelta 本次以學生為維度的成功增量
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
