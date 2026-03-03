package com.sp.system.service.notification;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sp.system.entity.notification.NotificationAnswer;

import java.util.List;

/**
 * 通知回答Service接口
 *
 */
public interface INotificationAnswerService extends IService<NotificationAnswer> {
    /**
     * 根据通知ID和用户ID查询回答列表
     *
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @return 回答集合
     */
    List<NotificationAnswer> selectByNotificationIdAndUserId(Long notificationId, Long userId);

    /**
     * 根据问题ID查询回答列表
     *
     * @param questionId 问题ID
     * @return 回答集合
     */
    List<NotificationAnswer> selectByQuestionId(Long questionId);

    /**
     * 根据通知ID删除回答
     *
     * @param notificationId 通知ID
     * @return 结果
     */
    int deleteByNotificationId(Long notificationId);
}