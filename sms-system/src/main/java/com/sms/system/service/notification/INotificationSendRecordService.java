package com.sms.system.service.notification;

import com.sms.system.entity.notification.NotificationSendRecord;

/**
 * 通知发送记录 Service 接口
 *
 */
public interface INotificationSendRecordService {
    /**
     * 新增发送记录
     *
     * @param sendRecord 发送记录
     * @return 结果
     */
    int save(NotificationSendRecord sendRecord);
}
