package com.sms.system.service.impl.notification;

import com.sms.system.entity.notification.NotificationReminderRecord;
import com.sms.system.mapper.notification.NotificationReminderRecordMapper;
import com.sms.system.service.notification.INotificationReminderRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通知提醒記錄 Service 業務層處理
 */
@Service
public class NotificationReminderRecordServiceImpl implements INotificationReminderRecordService {

    @Autowired
    private NotificationReminderRecordMapper notificationReminderRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSave(List<NotificationReminderRecord> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        return notificationReminderRecordMapper.batchInsert(list);
    }
}
