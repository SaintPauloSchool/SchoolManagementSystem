package com.sp.system.service.impl.notification;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sp.system.mapper.notification.NotificationReceiverMapper;
import com.sp.system.entity.notification.NotificationReceiver;
import com.sp.system.service.notification.INotificationReceiverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知接收对象Service业务层处理
 *
 */
@Service
public class NotificationReceiverServiceImpl extends ServiceImpl<NotificationReceiverMapper, NotificationReceiver> implements INotificationReceiverService {

    @Autowired
    private NotificationReceiverMapper notificationReceiverMapper;

    /**
     * 根据通知 ID 查询接收对象列表
     *
     * @param notificationId 通知 ID
     * @return 接收对象集合
     */
    @Override
    public List<NotificationReceiver> selectByNotificationId(Long notificationId) {
        return notificationReceiverMapper.selectByNotificationId(notificationId);
    }
}