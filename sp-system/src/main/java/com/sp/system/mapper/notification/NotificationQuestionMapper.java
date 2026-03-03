package com.sp.system.mapper.notification;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sp.system.entity.notification.NotificationQuestion;

import java.util.List;

/**
 * 通知问题Mapper接口
 *
 */
public interface NotificationQuestionMapper extends BaseMapper<NotificationQuestion> {
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
}