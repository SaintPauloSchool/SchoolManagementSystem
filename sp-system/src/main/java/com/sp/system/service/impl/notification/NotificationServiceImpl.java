package com.sp.system.service.impl.notification;

import com.sp.system.mapper.notification.NotificationMapper;
import com.sp.system.entity.notification.Notification;
import com.sp.system.service.notification.INotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知 Service 业务层处理
 *
 */
@Service
public class NotificationServiceImpl implements INotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    /**
     * 查询通知列表
     *
     * @param notification 通知信息
     * @return 通知集合
     */
    @Override
    public List<Notification> selectNotificationList(Notification notification) {
        return notificationMapper.selectNotificationList(notification);
    }

    /**
     * 查询通知详细信息
     *
     * @param notificationId 通知主键
     * @return 通知信息
     */
    @Override
    public Notification selectNotificationById(Long notificationId) {
        return notificationMapper.selectById(notificationId);
    }

    /**
     * 新增通知
     *
     * @param notification 通知信息
     * @return 结果
     */
    @Override
    public int insertNotification(Notification notification) {
        return notificationMapper.insert(notification);
    }

    /**
     * 修改通知
     *
     * @param notification 通知信息
     * @return 结果
     */
    @Override
    public int updateNotification(Notification notification) {
        return notificationMapper.updateById(notification);
    }

    /**
     * 删除通知
     *
     * @param notificationId 通知主键
     * @return 结果
     */
    @Override
    public int deleteNotificationById(Long notificationId) {
        return notificationMapper.deleteById(notificationId);
    }

    /**
     * 批量删除通知
     *
     * @param notificationIds 需要删除的通知主键集合
     * @return 结果
     */
    @Override
    public int deleteNotificationByIds(List<Long> notificationIds) {
        return notificationMapper.deleteBatchIds(notificationIds);
    }

    /**
     * 根据用户 ID 查询抄送给我的通知列表
     *
     * @param notification 通知信息（包含 userId, userType, publishDate）
     * @return 通知集合
     */
    @Override
    public List<Notification> selectCcToMeList(Notification notification) {
        return notificationMapper.selectCcToMeList(notification);
    }

    /**
     * 根据用户 ID 查询我发送的通知列表
     *
     * @param notification 通知信息（包含 senderId, publishDate）
     * @return 通知集合
     */
    @Override
    public List<Notification> selectMySendList(Notification notification) {
        return notificationMapper.selectMySendList(notification);
    }
    
    /**
     * 保存通知
     *
     * @param notification 通知
     * @return 结果
     */
    @Override
    public boolean save(Notification notification) {
        return insertNotification(notification) > 0;
    }
}