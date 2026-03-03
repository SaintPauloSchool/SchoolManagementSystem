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
     * 根据通知ID查询问题列表
     *
     * @param notificationId 通知ID
     * @return 问题集合
     */
    List<NotificationQuestion> selectByNotificationId(Long notificationId);

    /**
     * 根据父问题ID查询子问题列表
     *
     * @param parentQuestionId 父问题ID
     * @return 子问题集合
     */
    List<NotificationQuestion> selectByParentQuestionId(Long parentQuestionId);

    /**
     * 根据通知ID删除问题
     *
     * @param notificationId 通知ID
     * @return 结果
     */
    int deleteByNotificationId(Long notificationId);
}