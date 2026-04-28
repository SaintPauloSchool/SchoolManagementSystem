package com.sms.system.service.impl.notification;

import com.sms.system.entity.notification.NotificationReminderRecord;
import com.sms.system.mapper.notification.NotificationReminderRecordMapper;
import com.sms.system.service.notification.INotificationReminderRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知提醒记录 Service 业务层处理
 */
@Service
public class NotificationReminderRecordServiceImpl implements INotificationReminderRecordService {

    @Autowired
    private NotificationReminderRecordMapper notificationReminderRecordMapper;

    /**
     * 新增提醒记录
     *
     * @param reminderRecord 提醒记录
     * @return 结果
     */
    @Override
    public int save(NotificationReminderRecord reminderRecord) {
        return notificationReminderRecordMapper.insert(reminderRecord);
    }

    /**
     * 批量新增提醒记录
     *
     * @param list 提醒记录列表
     * @return 结果
     */
    @Override
    public int batchSave(List<NotificationReminderRecord> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        return notificationReminderRecordMapper.batchInsert(list);
    }

    /**
     * 根据通知ID查询提醒记录列表
     *
     * @param notificationId 通知ID
     * @return 提醒记录列表
     */
    @Override
    public List<NotificationReminderRecord> selectByNotificationId(Long notificationId) {
        return notificationReminderRecordMapper.selectByNotificationId(notificationId);
    }
}
