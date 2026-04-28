package com.sms.system.service.notification;

import com.sms.system.entity.notification.NotificationReminderRecord;

import java.util.List;

/**
 * 通知提醒记录 Service 接口
 */
public interface INotificationReminderRecordService {
    /**
     * 新增提醒记录
     *
     * @param reminderRecord 提醒记录
     * @return 结果
     */
    int save(NotificationReminderRecord reminderRecord);

    /**
     * 批量新增提醒记录
     *
     * @param list 提醒记录列表
     * @return 结果
     */
    int batchSave(List<NotificationReminderRecord> list);

    /**
     * 根据通知ID查询提醒记录列表
     *
     * @param notificationId 通知ID
     * @return 提醒记录列表
     */
    List<NotificationReminderRecord> selectByNotificationId(Long notificationId);
}
