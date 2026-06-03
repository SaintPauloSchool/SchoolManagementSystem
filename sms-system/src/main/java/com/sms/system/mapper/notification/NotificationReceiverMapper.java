package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationReceiver;

import java.util.List;

/**
 * 通知接收對象 Mapper 接口
 *
 */
public interface NotificationReceiverMapper {
    /**
     * 根據通知 ID 查詢接收對象列表
     *
     * @param notificationId 通知 ID
     * @return 接收對象集合
     */
    List<NotificationReceiver> selectByNotificationId(Long notificationId);
    
    /**
     * 新增接收對象
     *
     * @param receiver 接收對象
     * @return 結果
     */
    int insert(NotificationReceiver receiver);
}
