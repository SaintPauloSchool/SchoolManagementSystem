package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationAnswer;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知回答 Mapper 接口
 */
public interface NotificationAnswerMapper {
    /**
     * 根据通知ID查询回答列表
     *
     * @param notificationId 通知ID
     * @return 回答集合
     */
    List<NotificationAnswer> selectByNotificationId(Long notificationId);

    /**
     * 根据通知ID和用户ID查询回答列表
     *
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @return 回答集合
     */
    List<NotificationAnswer> selectByNotificationIdAndUserId(@Param("notificationId") Long notificationId, @Param("userId") String userId);

}
