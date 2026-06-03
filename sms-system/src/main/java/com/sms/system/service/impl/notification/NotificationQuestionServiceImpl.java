package com.sms.system.service.impl.notification;

import com.sms.system.mapper.notification.NotificationQuestionMapper;
import com.sms.system.entity.notification.NotificationQuestion;
import com.sms.system.service.notification.INotificationQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知問題 Service 業務層處理
 *
 */
@Service
public class NotificationQuestionServiceImpl implements INotificationQuestionService {

    @Autowired
    private NotificationQuestionMapper notificationQuestionMapper;

    /**
     * 根據通知 ID 查詢問題列表
     *
     * @param notificationId 通知 ID
     * @return 問題集合
     */
    @Override
    public List<NotificationQuestion> selectByNotificationId(Long notificationId) {
        return notificationQuestionMapper.selectByNotificationId(notificationId);
    }
    
    /**
     * 新增問題
     *
     * @param question 問題
     * @return 結果
     */
    @Override
    public int save(NotificationQuestion question) {
        return notificationQuestionMapper.insert(question);
    }
}
