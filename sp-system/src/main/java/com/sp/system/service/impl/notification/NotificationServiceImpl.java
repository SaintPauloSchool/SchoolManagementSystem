package com.sp.system.service.impl.notification;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sp.system.mapper.notification.NotificationMapper;
import com.sp.system.entity.notification.Notification;
import com.sp.system.service.notification.INotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知Service业务层处理
 *
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {

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
     * 根据用户ID查询抄送给我的通知列表
     *
     * @param userId 用户ID
     * @param userType 用户类型
     * @return 通知集合
     */
    @Override
    public List<Notification> selectCcToMeList(Long userId, String userType) {
        return notificationMapper.selectCcToMeList(userId, userType);
    }

    /**
     * 根据用户ID查询我发送的通知列表
     *
     * @param senderId 发送人ID
     * @return 通知集合
     */
    @Override
    public List<Notification> selectMySendList(Long senderId) {
        return notificationMapper.selectMySendList(senderId);
    }
}