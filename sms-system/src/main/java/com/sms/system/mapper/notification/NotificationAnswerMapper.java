package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationAnswer;

import java.util.List;

/**
 * 通知回答 Mapper 接口
 */
public interface NotificationAnswerMapper {

    /**
     * 根據通知ID查詢回答列表
     *
     * @param notificationId 通知ID
     * @return 回答集合
     */
    List<NotificationAnswer> selectByNotificationId(Long notificationId);
}
