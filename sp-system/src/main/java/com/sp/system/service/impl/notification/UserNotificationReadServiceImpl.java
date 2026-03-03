package com.sp.system.service.impl.notification;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sp.system.mapper.notification.UserNotificationReadMapper;
import com.sp.system.entity.notification.UserNotificationRead;
import com.sp.system.service.notification.IUserNotificationReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户通知阅读状态Service业务层处理
 *
 */
@Service
public class UserNotificationReadServiceImpl extends ServiceImpl<UserNotificationReadMapper, UserNotificationRead> implements IUserNotificationReadService {

    @Autowired
    private UserNotificationReadMapper userNotificationReadMapper;

    /**
     * 根据通知ID查询阅读状态列表
     *
     * @param notificationId 通知ID
     * @return 阅读状态集合
     */
    @Override
    public List<UserNotificationRead> selectByNotificationId(Long notificationId) {
        return userNotificationReadMapper.selectByNotificationId(notificationId);
    }

    /**
     * 根据用户ID查询阅读状态列表
     *
     * @param userId 用户ID
     * @param userType 用户类型
     * @return 阅读状态集合
     */
    @Override
    public List<UserNotificationRead> selectByUserId(Long userId, String userType) {
        return userNotificationReadMapper.selectByUserId(userId, userType);
    }

    /**
     * 根据通知ID和用户ID查询阅读状态
     *
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @param userType 用户类型
     * @return 阅读状态
     */
    @Override
    public UserNotificationRead selectByNotificationIdAndUserId(Long notificationId, Long userId, String userType) {
        List<UserNotificationRead> reads = selectByNotificationId(notificationId);
        if (reads != null) {
            return reads.stream()
                .filter(read -> read.getUserId().equals(userId) && read.getUserType().equals(userType))
                .findFirst()
                .orElse(null);
        }
        return null;
    }
}