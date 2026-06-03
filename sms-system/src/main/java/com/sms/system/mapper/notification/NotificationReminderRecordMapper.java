package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationReminderRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知提醒記錄 Mapper 接口
 */
public interface NotificationReminderRecordMapper {
    /**
     * 新增提醒記錄
     *
     * @param reminderRecord 提醒記錄
     * @return 結果
     */
    int insert(NotificationReminderRecord reminderRecord);

    /**
     * 批量新增提醒記錄
     *
     * @param list 提醒記錄列表
     * @return 結果
     */
    int batchInsert(@Param("list") List<NotificationReminderRecord> list);

    /**
     * 根據通知ID查詢提醒記錄列表
     *
     * @param notificationId 通知ID
     * @return 提醒記錄列表
     */
    List<NotificationReminderRecord> selectByNotificationId(Long notificationId);
}
