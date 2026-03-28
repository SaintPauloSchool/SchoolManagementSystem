package com.sp.system.service.notification;

import com.sp.system.entity.notification.Notification;

import java.util.List;

/**
 * 通知 Service 接口
 *
 */
public interface INotificationService {
    /**
     * 查询通知列表
     *
     * @param notification 通知信息
     * @return 通知集合
     */
    List<Notification> selectNotificationList(Notification notification);

    /**
     * 查询通知详细信息
     *
     * @param notificationId 通知主键
     * @return 通知信息
     */
    Notification selectNotificationById(Long notificationId);

    /**
     * 新增通知
     *
     * @param notification 通知信息
     * @return 结果
     */
    int insertNotification(Notification notification);

    /**
     * 修改通知
     *
     * @param notification 通知信息
     * @return 结果
     */
    int updateNotification(Notification notification);

    /**
     * 删除通知
     *
     * @param notificationId 通知主键
     * @return 结果
     */
    int deleteNotificationById(Long notificationId);

    /**
     * 批量删除通知
     *
     * @param notificationIds 需要删除的通知主键集合
     * @return 结果
     */
    int deleteNotificationByIds(List<Long> notificationIds);

    /**
     * 根据用户 ID 查询抄送给我的通知列表
     *
     * @param userId 用户 ID
     * @param userType 用户类型
     * @return 通知集合
     */
    List<Notification> selectCcToMeList(Long userId, String userType);

    /**
     * 根据用户 ID 查询我发送的通知列表
     *
     * @param senderId 发送人 ID
     * @return 通知集合
     */
    List<Notification> selectMySendList(Long senderId);
    
    /**
     * 保存通知
     *
     * @param notification 通知
     * @return 结果
     */
    boolean save(Notification notification);
}