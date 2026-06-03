package com.sms.system.service.notification;

import com.sms.system.entity.notification.NotificationQuestion;

import java.util.List;

/**
 * 通知問題 Service 接口
 *
 */
public interface INotificationQuestionService {
    /**
     * 根據通知 ID 查詢問題列表
     *
     * @param notificationId 通知 ID
     * @return 問題集合
     */
    List<NotificationQuestion> selectByNotificationId(Long notificationId);
    
    /**
     * 新增問題
     *
     * @param question 問題
     * @return 結果
     */
    int save(NotificationQuestion question);
}
