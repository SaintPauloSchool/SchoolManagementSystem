package com.sp.system.mapper.notification;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sp.system.entity.notification.NotificationReceiver;

import java.util.List;

/**
 * 通知接收对象Mapper接口
 *
 */
public interface NotificationReceiverMapper extends BaseMapper<NotificationReceiver> {
    /**
     * 根据通知ID查询接收对象列表
     *
     * @param notificationId 通知ID
     * @return 接收对象集合
     */
    List<NotificationReceiver> selectByNotificationId(Long notificationId);
}