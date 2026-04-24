package com.sms.system.service.notification;

import com.sms.system.entity.notification.NotificationUserReadRecord;

import java.util.List;

/**
 * 通知用户阅读记录 Service 接口
 *
 */
public interface INotificationUserReadRecordService {
    /**
     * 批量新增阅读记录
     *
     * @param readRecords 阅读记录列表
     * @return 结果
     */
    int batchSave(List<NotificationUserReadRecord> readRecords);
}
