package com.sms.system.service.notification;

import com.sms.system.entity.notification.Notification;

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
     * 根据用户 ID 查询抄送给我的通知列表
     *
     * @param notification 通知信息（包含 userId, userType, publishDate）
     * @return 通知集合
     */
    List<Notification> selectCcToMeList(Notification notification);

    /**
     * 根据用户 ID 查询我发送的通知列表
     *
     * @param notification 通知信息（包含 senderId, publishDate）
     * @return 通知集合
     */
    List<Notification> selectMySendList(Notification notification);
    
    /**
     * 保存通知
     *
     * @param notification 通知
     * @return 结果
     */
    boolean save(Notification notification);
}
