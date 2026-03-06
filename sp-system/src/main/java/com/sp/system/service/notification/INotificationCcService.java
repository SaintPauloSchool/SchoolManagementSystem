package com.sp.system.service.notification;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sp.system.entity.notification.NotificationCc;

import java.util.List;

/**
 * 通知抄送对象Service接口
 *
 */
public interface INotificationCcService extends IService<NotificationCc> {
    /**
     * 根据通知 ID 查询抄送对象列表
     *
     * @param notificationId 通知 ID
     * @return 抄送对象集合
     */
    List<NotificationCc> selectByNotificationId(Long notificationId);
}