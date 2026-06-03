package com.sms.system.service.impl.notification;

import com.sms.system.entity.notification.NotificationReminderRecord;
import com.sms.system.mapper.notification.NotificationReminderRecordMapper;
import com.sms.system.service.notification.INotificationReminderRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知提醒記錄 Service 業務層處理
 */
@Service
public class NotificationReminderRecordServiceImpl implements INotificationReminderRecordService {

    @Autowired
    private NotificationReminderRecordMapper notificationReminderRecordMapper;

    /**
     * 新增提醒記錄
     *
     * @param reminderRecord 提醒記錄
     * @return 結果
     */
    @Override
    public int save(NotificationReminderRecord reminderRecord) {
        return notificationReminderRecordMapper.insert(reminderRecord);
    }

    /**
     * 批量新增提醒記錄
     *
     * @param list 提醒記錄列表
     * @return 結果
     */
    @Override
    public int batchSave(List<NotificationReminderRecord> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        return notificationReminderRecordMapper.batchInsert(list);
    }

    /**
     * 根據通知ID查詢提醒記錄列表
     *
     * @param notificationId 通知ID
     * @return 提醒記錄列表
     */
    @Override
    public List<NotificationReminderRecord> selectByNotificationId(Long notificationId) {
        return notificationReminderRecordMapper.selectByNotificationId(notificationId);
    }
}
