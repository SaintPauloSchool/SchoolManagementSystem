package com.sms.system.service.impl.notification;

import com.sms.system.entity.notification.NotificationSendRecord;
import com.sms.system.mapper.notification.NotificationSendRecordMapper;
import com.sms.system.service.notification.INotificationSendRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 通知发送记录 Service 业务层处理
 *
 */
@Service
public class NotificationSendRecordServiceImpl implements INotificationSendRecordService {

    @Autowired
    private NotificationSendRecordMapper notificationSendRecordMapper;

    /**
     * 新增发送记录
     *
     * @param sendRecord 发送记录
     * @return 结果
     */
    @Override
    public int save(NotificationSendRecord sendRecord) {
        return notificationSendRecordMapper.insert(sendRecord);
    }
}
