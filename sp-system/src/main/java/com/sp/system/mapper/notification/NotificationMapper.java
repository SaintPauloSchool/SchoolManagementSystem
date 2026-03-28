package com.sp.system.mapper.notification;

import com.sp.system.entity.notification.Notification;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知 Mapper 接口
 *
 */
public interface NotificationMapper {
    /**
     * 查询通知列表
     *
     * @param notification 通知信息
     * @return 通知集合
     */
    List<Notification> selectNotificationList(Notification notification);

    /**
     * 根据用户 ID 查询抄送给我的通知列表
     *
     * @param userId 用户 ID
     * @param userType 用户类型
     * @return 通知集合
     */
    List<Notification> selectCcToMeList(@Param("userId") Long userId, @Param("userType") String userType);

    /**
     * 根据用户 ID 查询我发送的通知列表
     *
     * @param senderId 发送人 ID
     * @return 通知集合
     */
    List<Notification> selectMySendList(@Param("senderId") Long senderId);
    
    /**
     * 新增通知
     *
     * @param notification 通知
     * @return 结果
     */
    int insert(Notification notification);
    
    /**
     * 根据 ID 查询通知
     *
     * @param notificationId 通知 ID
     * @return 通知
     */
    Notification selectById(Long notificationId);
    
    /**
     * 修改通知
     *
     * @param notification 通知
     * @return 结果
     */
    int updateById(Notification notification);
    
    /**
     * 根据 ID 删除通知
     *
     * @param notificationId 通知 ID
     * @return 结果
     */
    int deleteById(Long notificationId);
    
    /**
     * 批量删除通知
     *
     * @param notificationIds 通知 ID 集合
     * @return 结果
     */
    int deleteBatchIds(List<Long> notificationIds);
}