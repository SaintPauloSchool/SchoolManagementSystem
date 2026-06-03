package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationCc;

import java.util.List;

/**
 * 通知抄送對象 Mapper 接口
 *
 */
public interface NotificationCcMapper {
    /**
     * 根據通知 ID 查詢抄送對象列表
     *
     * @param notificationId 通知 ID
     * @return 抄送對象集合
     */
    List<NotificationCc> selectByNotificationId(Long notificationId);
    
    /**
     * 新增抄送對象
     *
     * @param cc 抄送對象
     * @return 結果
     */
    int insert(NotificationCc cc);

    /**
     * 查詢所有抄送記錄
     *
     * @return 抄送對象集合
     */
    List<NotificationCc> selectAll();
}
