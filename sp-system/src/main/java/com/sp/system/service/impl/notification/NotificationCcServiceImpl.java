package com.sp.system.service.impl.notification;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sp.system.mapper.notification.NotificationCcMapper;
import com.sp.system.entity.notification.NotificationCc;
import com.sp.system.service.notification.INotificationCcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知抄送对象Service业务层处理
 *
 */
@Service
public class NotificationCcServiceImpl extends ServiceImpl<NotificationCcMapper, NotificationCc> implements INotificationCcService {

    @Autowired
    private NotificationCcMapper notificationCcMapper;

    /**
     * 根据通知ID查询抄送对象列表
     *
     * @param notificationId 通知ID
     * @return 抄送对象集合
     */
    @Override
    public List<NotificationCc> selectByNotificationId(Long notificationId) {
        return notificationCcMapper.selectByNotificationId(notificationId);
    }

    /**
     * 根据通知ID删除抄送对象
     *
     * @param notificationId 通知ID
     * @return 结果
     */
    @Override
    public int deleteByNotificationId(Long notificationId) {
        List<NotificationCc> ccs = selectByNotificationId(notificationId);
        if (ccs != null && !ccs.isEmpty()) {
            return notificationCcMapper.deleteBatchIds(
                ccs.stream().map(NotificationCc::getCcId).collect(java.util.stream.Collectors.toList())
            );
        }
        return 0;
    }
}