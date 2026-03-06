package com.sp.system.service.impl.notification;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sp.system.mapper.notification.NotificationQuestionMapper;
import com.sp.system.entity.notification.NotificationQuestion;
import com.sp.system.service.notification.INotificationQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知问题Service业务层处理
 *
 */
@Service
public class NotificationQuestionServiceImpl extends ServiceImpl<NotificationQuestionMapper, NotificationQuestion> implements INotificationQuestionService {

    @Autowired
    private NotificationQuestionMapper notificationQuestionMapper;

    /**
     * 根据通知 ID 查询问题列表
     *
     * @param notificationId 通知 ID
     * @return 问题集合
     */
    @Override
    public List<NotificationQuestion> selectByNotificationId(Long notificationId) {
        return notificationQuestionMapper.selectByNotificationId(notificationId);
    }
}