package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationReminderRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知提醒記錄 Mapper 接口
 */
public interface NotificationReminderRecordMapper {

    /**
     * 批量新增提醒記錄
     *
     * @param list 提醒記錄列表
     * @return 結果
     */
    int batchInsert(@Param("list") List<NotificationReminderRecord> list);
}
