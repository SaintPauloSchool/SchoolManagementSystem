package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationCc;

import java.util.List;

/**
 * 通知抄送对象 Mapper 接口
 *
 */
public interface NotificationCcMapper {
    /**
     * 根据通知 ID 查询抄送对象列表
     *
     * @param notificationId 通知 ID
     * @return 抄送对象集合
     */
    List<NotificationCc> selectByNotificationId(Long notificationId);
    
    /**
     * 新增抄送对象
     *
     * @param cc 抄送对象
     * @return 结果
     */
    int insert(NotificationCc cc);
}
