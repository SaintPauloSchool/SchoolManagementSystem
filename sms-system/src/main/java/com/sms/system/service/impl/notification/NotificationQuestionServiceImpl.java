package com.sms.system.service.impl.notification;

import com.sms.system.mapper.notification.NotificationQuestionMapper;
import com.sms.system.entity.notification.NotificationQuestion;
import com.sms.system.service.notification.INotificationQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知问题 Service 业务层处理
 *
 */
@Service
public class NotificationQuestionServiceImpl implements INotificationQuestionService {

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
    
    /**
     * 新增问题
     *
     * @param question 问题
     * @return 结果
     */
    @Override
    public int save(NotificationQuestion question) {
        return notificationQuestionMapper.insert(question);
    }
}
