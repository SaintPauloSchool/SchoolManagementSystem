package com.sms.system.service.impl.notification;

import com.sms.system.entity.notification.Notification;
import com.sms.system.entity.notification.NotificationReminderRecord;
import com.sms.system.entity.notification.NotificationResendFailRecord;
import com.sms.system.entity.notification.NotificationSendRecord;
import com.sms.system.entity.notification.NotificationUserReadRecord;
import com.sms.system.entity.notification.SendResult;
import com.sms.system.entity.notification.receiver.NotificationReceiverStats;
import com.sms.system.entity.notification.receiver.NotificationReceiverTarget;
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
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通知發佈相關持久化 Service 實現。
 * <p>接收人資訊由上游 {@code resolveReceivers} 解析後傳入，寫庫時不再查庫。</p>
 */
@Service
public class NotificationPublishRecordServiceImpl implements INotificationPublishRecordService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublishRecordServiceImpl.class);

    private static final String SEND_STATUS_SUCCESS = "2";
    private static final String SEND_STATUS_FAIL = "3";
    private static final String SEND_STATUS_PARTIAL = "4";
    private static final String USER_TYPE_STUDENT_PARENT = "1";

    @Autowired
    private INotificationSendRecordService notificationSendRecordService;

    @Autowired
    private INotificationUserReadRecordService notificationUserReadRecordService;

    @Autowired
    private INotificationReminderRecordService notificationReminderRecordService;

    @Autowired
    private INotificationResendFailRecordService notificationResendFailRecordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePublishRecords(Notification notification,
                                   SendResult sendResult,
                                   List<NotificationReceiverTarget> receiverTargets) {
        NotificationSendRecord sendRecord = buildSendRecord(notification, sendResult, receiverTargets);
        notificationSendRecordService.save(sendRecord);

        List<NotificationUserReadRecord> readRecords = buildUserReadRecords(
                sendRecord.getSendRecordId(), receiverTargets, sendResult.getSuccessUserIds());
        notificationUserReadRecordService.batchSave(readRecords);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveReminderRecords(List<NotificationReminderRecord> reminderRecords) {
        if (reminderRecords == null || reminderRecords.isEmpty()) {
            return;
        }
        notificationReminderRecordService.batchSave(reminderRecords);
    }

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
     * 按學籍 {@code student_id} 維度統計發送匯總；同一學生多個家長時，任一家長發送成功即計為成功。
     */
    private NotificationSendRecord buildSendRecord(Notification notification,
                                                   SendResult sendResult,
                                                   List<NotificationReceiverTarget> receiverTargets) {
        NotificationSendRecord sendRecord = new NotificationSendRecord();
        sendRecord.setNotificationId(notification.getNotificationId());
        sendRecord.setSenderId(notification.getSenderId());
        sendRecord.setSenderName(notification.getSenderName());
        sendRecord.setSendTime(LocalDateTime.now());

        Map<String, Set<String>> studentToParents = NotificationReceiverStats.groupParentsByStudent(receiverTargets);
        Set<String> successUserIds = sendResult.getSuccessUserIds() != null
                ? sendResult.getSuccessUserIds() : Collections.emptySet();

        int totalCount = studentToParents.size();
        int[] counts = NotificationReceiverStats.countStudentResults(studentToParents, successUserIds);
        int successCount = counts[0];
        int failCount = counts[1];

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

    private List<NotificationUserReadRecord> buildUserReadRecords(Long sendRecordId,
                                                                  List<NotificationReceiverTarget> receiverTargets,
                                                                  Set<String> successUserIds) {
        if (receiverTargets == null || receiverTargets.isEmpty()) {
            return Collections.emptyList();
        }
        List<NotificationUserReadRecord> readRecords = new ArrayList<>(receiverTargets.size());
        LocalDateTime now = LocalDateTime.now();
        Set<String> processedKeys = new HashSet<>();

        for (NotificationReceiverTarget target : receiverTargets) {
            if (!StringUtils.hasText(target.getParentUserId())) {
                continue;
            }
            String key = target.getParentUserId() + "_"
                    + NotificationReceiverStats.studentStatsKey(target) + "_"
                    + (target.getDepartmentId() != null ? target.getDepartmentId() : "null");
            if (!processedKeys.add(key)) {
                continue;
            }
            readRecords.add(createReadRecord(
                    sendRecordId,
                    target.getParentUserId(),
                    target.getStudentId(),
                    target.getDepartmentId(),
                    successUserIds != null && successUserIds.contains(target.getParentUserId()),
                    now));
        }
        return readRecords;
    }

    private NotificationUserReadRecord createReadRecord(Long sendRecordId, String parentUserId,
                                                        String studentId, Long departmentId,
                                                        boolean sendSuccess, LocalDateTime createTime) {
        NotificationUserReadRecord record = new NotificationUserReadRecord();
        record.setSendRecordId(sendRecordId);
        record.setUserId(parentUserId);
        record.setUserType(USER_TYPE_STUDENT_PARENT);
        record.setIsRead("0");
        record.setReplyStatus("0");
        record.setSendStatus(sendSuccess ? "1" : "0");
        record.setStudentId(studentId);
        record.setDepartmentId(departmentId);
        record.setCreateTime(createTime);
        return record;
    }

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
                failRecord.setStudentId(record.getStudentId());
                String reason = failedUserReasons.getOrDefault(record.getUserId(), "未知原因");
                failRecord.setFailReason1("自動重發失敗");
                failRecord.setFailMessage1(reason);
                notificationResendFailRecordService.saveOrUpdate(failRecord);
            }
        }
    }

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
