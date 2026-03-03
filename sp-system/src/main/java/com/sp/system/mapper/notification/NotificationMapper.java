package com.sp.system.mapper.notification;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sp.system.entity.notification.Notification;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知Mapper接口
 *
 */
public interface NotificationMapper extends BaseMapper<Notification> {
    /**
     * 查询通知列表
     *
     * @param notification 通知信息
     * @return 通知集合
     */
    List<Notification> selectNotificationList(Notification notification);

    /**
     * 根据用户ID查询抄送给我的通知列表
     *
     * @param userId 用户ID
     * @param userType 用户类型
     * @return 通知集合
     */
    List<Notification> selectCcToMeList(@Param("userId") Long userId, @Param("userType") String userType);

    /**
     * 根据用户ID查询我发送的通知列表
     *
     * @param senderId 发送人ID
     * @return 通知集合
     */
    List<Notification> selectMySendList(@Param("senderId") Long senderId);
}