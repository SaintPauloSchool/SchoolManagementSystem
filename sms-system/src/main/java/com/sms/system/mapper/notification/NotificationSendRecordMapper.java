package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationSendRecord;

/**
 * 通知发送记录 Mapper 接口
 *
 */
public interface NotificationSendRecordMapper {
    /**
     * 新增发送记录
     *
     * @param sendRecord 发送记录
     * @return 结果
     */
    int insert(NotificationSendRecord sendRecord);

}
