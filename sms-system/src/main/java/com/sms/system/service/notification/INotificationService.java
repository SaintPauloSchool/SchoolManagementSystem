package com.sms.system.service.notification;

import com.sms.system.entity.notification.Notification;

import java.util.List;

/**
 * 通知 Service 接口
 *
 */
public interface INotificationService {
    /**
     * 查詢通知列表
     *
     * @param notification 通知信息
     * @return 通知集合
     */
    List<Notification> selectNotificationList(Notification notification);

    /**
     * 查詢通知詳細信息
     *
     * @param notificationId 通知主鍵
     * @return 通知信息
     */
    Notification selectNotificationById(Long notificationId);

    /**
     * 根據用戶 ID 查詢抄送給我的通知列表
     *
     * @param notification 通知信息（包含 userId, userType, publishDate）
     * @return 通知集合
     */
    List<Notification> selectCcToMeList(Notification notification);

    /**
     * 根據用戶 ID 查詢我發送的通知列表
     *
     * @param notification 通知信息（包含 senderId, publishDate）
     * @return 通知集合
     */
    List<Notification> selectMySendList(Notification notification);
    
    /**
     * 保存通知
     *
     * @param notification 通知
     * @return 結果
     */
    boolean save(Notification notification);
}
