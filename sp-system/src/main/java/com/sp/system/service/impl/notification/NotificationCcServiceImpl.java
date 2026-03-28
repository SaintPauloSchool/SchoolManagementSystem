package com.sp.system.service.impl.notification;

import com.sp.system.mapper.notification.NotificationCcMapper;
import com.sp.system.entity.notification.NotificationCc;
import com.sp.system.service.notification.INotificationCcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知抄送对象 Service 业务层处理
 *
 */
@Service
public class NotificationCcServiceImpl implements INotificationCcService {

    @Autowired
    private NotificationCcMapper notificationCcMapper;

    /**
     * 根据通知 ID 查询抄送对象列表
     *
     * @param notificationId 通知 ID
     * @return 抄送对象集合
     */
    @Override
    public List<NotificationCc> selectByNotificationId(Long notificationId) {
        return notificationCcMapper.selectByNotificationId(notificationId);
    }
    
    /**
     * 新增抄送对象
     *
     * @param cc 抄送对象
     * @return 结果
     */
    @Override
    public int save(NotificationCc cc) {
        return notificationCcMapper.insert(cc);
    }
}