package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationReminderRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知提醒记录 Mapper 接口
 */
public interface NotificationReminderRecordMapper {
    /**
     * 新增提醒记录
     *
     * @param reminderRecord 提醒记录
     * @return 结果
     */
    int insert(NotificationReminderRecord reminderRecord);

    /**
     * 批量新增提醒记录
     *
     * @param list 提醒记录列表
     * @return 结果
     */
    int batchInsert(@Param("list") List<NotificationReminderRecord> list);

    /**
     * 根据通知ID查询提醒记录列表
     *
     * @param notificationId 通知ID
     * @return 提醒记录列表
     */
    List<NotificationReminderRecord> selectByNotificationId(Long notificationId);
}
