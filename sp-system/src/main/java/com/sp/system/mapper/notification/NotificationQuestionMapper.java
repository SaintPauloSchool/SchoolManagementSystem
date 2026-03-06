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
     * 根据通知 ID 查询问题列表
     *
     * @param notificationId 通知 ID
     * @return 问题集合
     */
    List<NotificationQuestion> selectByNotificationId(Long notificationId);
}