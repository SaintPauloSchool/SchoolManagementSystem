package com.sp.system.service.notification;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sp.system.entity.notification.UserNotificationRead;

import java.util.List;

/**
 * 用户通知阅读状态Service接口
 *
 */
public interface IUserNotificationReadService extends IService<UserNotificationRead> {
    /**
     * 根据通知ID查询阅读状态列表
     *
     * @param notificationId 通知ID
     * @return 阅读状态集合
     */
    List<UserNotificationRead> selectByNotificationId(Long notificationId);

    /**
     * 根据用户ID查询阅读状态列表
     *
     * @param userId 用户ID
     * @param userType 用户类型
     * @return 阅读状态集合
     */
    List<UserNotificationRead> selectByUserId(Long userId, String userType);

    /**
     * 根据通知ID和用户ID查询阅读状态
     *
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @param userType 用户类型
     * @return 阅读状态
     */
    UserNotificationRead selectByNotificationIdAndUserId(Long notificationId, Long userId, String userType);
}