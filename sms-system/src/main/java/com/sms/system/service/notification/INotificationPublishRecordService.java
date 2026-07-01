package com.sms.system.service.notification;

import com.sms.system.entity.SysSchoolFamilyContact;
import com.sms.system.entity.notification.Notification;
import com.sms.system.entity.notification.NotificationReminderRecord;
import com.sms.system.entity.notification.NotificationSendRecord;
import com.sms.system.entity.notification.NotificationUserReadRecord;
import com.sms.system.entity.notification.SendResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通知發佈相關的持久化 Service
 * 負責發送記錄、閱讀記錄、提醒記錄、重發結果等寫庫操作
 */
public interface INotificationPublishRecordService {

    /**
     * 保存發佈結果：發送記錄 + 用戶閱讀記錄（同一事務）
     */
    void savePublishRecords(Notification notification,
                            List<String> studentUserIds,
                            SendResult sendResult,
                            List<SysSchoolFamilyContact> relations,
                            List<String> parentUserIds,
                            Map<String, Long> studentDepartmentIds,
                            Map<String, String> parentStudentUserIds);

    /**
     * 批量保存提醒記錄
     */
    void saveReminderRecords(List<NotificationReminderRecord> reminderRecords);

    /**
     * 保存重發結果：更新閱讀記錄、自動重發失敗記錄、發送統計（同一事務）
     *
     * @param studentSuccessCount 以學生為維度的重發成功數，用於更新發送記錄統計
     */
    void saveResendRecords(Long notificationId,
                           NotificationSendRecord sendRecord,
                           List<NotificationUserReadRecord> failedRecords,
                           Set<String> overallSuccessUserIds,
                           Map<String, String> failedUserReasons,
                           boolean isAutoTask,
                           int studentSuccessCount);
}
