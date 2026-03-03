package com.sp.system.mapper.notification;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sp.system.entity.notification.NotificationCc;

import java.util.List;

/**
 * 通知抄送对象Mapper接口
 *
 */
public interface NotificationCcMapper extends BaseMapper<NotificationCc> {
    /**
     * 根据通知ID查询抄送对象列表
     *
     * @param notificationId 通知ID
     * @return 抄送对象集合
     */
    List<NotificationCc> selectByNotificationId(Long notificationId);
}