package com.sms.system.service.notification;

import com.sms.system.entity.notification.NotificationReminderRecord;

import java.util.List;

/**
 * 通知提醒記錄 Service 接口
 */
public interface INotificationReminderRecordService {

    /**
     * 批量新增提醒記錄
     *
     * @param list 提醒記錄列表
     * @return 結果
     */
    int batchSave(List<NotificationReminderRecord> list);
}
