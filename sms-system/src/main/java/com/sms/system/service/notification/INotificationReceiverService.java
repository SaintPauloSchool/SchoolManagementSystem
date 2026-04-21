package com.sms.system.service.notification;

import com.sms.system.entity.notification.NotificationReceiver;

import java.util.List;

/**
 * 通知接收对象 Service 接口
 *
 */
public interface INotificationReceiverService {
    /**
     * 根据通知 ID 查询接收对象列表
     *
     * @param notificationId 通知 ID
     * @return 接收对象集合
     */
    List<NotificationReceiver> selectByNotificationId(Long notificationId);
    
    /**
     * 新增接收对象
     *
     * @param receiver 接收对象
     * @return 结果
     */
    int save(NotificationReceiver receiver);
}
