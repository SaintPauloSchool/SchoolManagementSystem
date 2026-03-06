package com.sp.system.service.notification;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sp.system.entity.notification.NotificationQuestion;

import java.util.List;

/**
 * 通知问题Service接口
 *
 */
public interface INotificationQuestionService extends IService<NotificationQuestion> {
    /**
     * 根据通知 ID 查询问题列表
     *
     * @param notificationId 通知 ID
     * @return 问题集合
     */
    List<NotificationQuestion> selectByNotificationId(Long notificationId);
}