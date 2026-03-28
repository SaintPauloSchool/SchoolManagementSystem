package com.sp.system.service.notification;

import com.sp.system.entity.notification.NotificationQuestion;

import java.util.List;

/**
 * 通知问题 Service 接口
 *
 */
public interface INotificationQuestionService {
    /**
     * 根据通知 ID 查询问题列表
     *
     * @param notificationId 通知 ID
     * @return 问题集合
     */
    List<NotificationQuestion> selectByNotificationId(Long notificationId);
    
    /**
     * 新增问题
     *
     * @param question 问题
     * @return 结果
     */
    int save(NotificationQuestion question);
}