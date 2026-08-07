package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.Notification;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知 Mapper 接口
 *
 */
public interface NotificationMapper {
    /**
     * 查詢通知列表
     *
     * @param notification 通知資訊
     * @return 通知集合
     */
    List<Notification> selectNotificationList(Notification notification);

    /**
     * 根據用戶 ID 查詢抄送給我的通知列表
     *
     * @param notification 通知資訊（包含 userId, userType, publishDate）
     * @return 通知集合
     */
    List<Notification> selectCcToMeList(Notification notification);

    /**
     * 根據用戶 ID 查詢我發送的通知列表
     *
     * @param notification 通知資訊（包含 senderId, publishDate）
     * @return 通知集合
     */
    List<Notification> selectMySendList(Notification notification);
    
    /**
     * 新增通知
     *
     * @param notification 通知
     * @return 結果
     */
    int insert(Notification notification);
    
    /**
     * 根據 ID 查詢通知
     *
     * @param notificationId 通知 ID
     * @return 通知
     */
    Notification selectById(Long notificationId);

    /**
     * 更新通知狀態
     *
     * @param notificationId 通知 ID
     * @param status 狀態
     * @return 影響行數
     */
    int updateStatus(@Param("notificationId") Long notificationId,
                     @Param("status") String status,
                     @Param("updateBy") String updateBy);

}

