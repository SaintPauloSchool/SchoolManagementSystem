package com.sp.system.service.notification;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sp.system.entity.notification.NotificationReceiver;

import java.util.List;

/**
 * 通知接收对象Service接口
 *
 */
public interface INotificationReceiverService extends IService<NotificationReceiver> {
    /**
     * 根据通知 ID 查询接收对象列表
     *
     * @param notificationId 通知 ID
     * @return 接收对象集合
     */
    List<NotificationReceiver> selectByNotificationId(Long notificationId);
}