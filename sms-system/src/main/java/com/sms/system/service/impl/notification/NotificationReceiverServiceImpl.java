package com.sms.system.service.impl.notification;

import com.sms.system.mapper.notification.NotificationReceiverMapper;
import com.sms.system.entity.notification.NotificationReceiver;
import com.sms.system.service.notification.INotificationReceiverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知接收对象 Service 业务层处理
 *
 */
@Service
public class NotificationReceiverServiceImpl implements INotificationReceiverService {

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
    
    /**
     * 新增接收对象
     *
     * @param receiver 接收对象
     * @return 结果
     */
    @Override
    public int save(NotificationReceiver receiver) {
        return notificationReceiverMapper.insert(receiver);
    }
}
