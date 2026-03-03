package com.sp.system.mapper.notification;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sp.system.entity.notification.NotificationAnswer;

import java.util.List;

/**
 * 通知回答Mapper接口
 *
 */
public interface NotificationAnswerMapper extends BaseMapper<NotificationAnswer> {
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
}