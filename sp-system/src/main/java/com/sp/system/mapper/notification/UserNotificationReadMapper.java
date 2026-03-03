package com.sp.system.mapper.notification;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sp.system.entity.notification.UserNotificationRead;

import java.util.List;

/**
 * 用户通知阅读状态Mapper接口
 *
 */
public interface UserNotificationReadMapper extends BaseMapper<UserNotificationRead> {
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
}