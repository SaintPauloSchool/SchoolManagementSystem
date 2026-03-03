package com.sp.system.service.impl.notification;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sp.system.mapper.notification.NotificationAnswerMapper;
import com.sp.system.entity.notification.NotificationAnswer;
import com.sp.system.service.notification.INotificationAnswerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知回答Service业务层处理
 *
 */
@Service
public class NotificationAnswerServiceImpl extends ServiceImpl<NotificationAnswerMapper, NotificationAnswer> implements INotificationAnswerService {

    @Autowired
    private NotificationAnswerMapper notificationAnswerMapper;

    /**
     * 根据通知ID和用户ID查询回答列表
     *
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @return 回答集合
     */
    @Override
    public List<NotificationAnswer> selectByNotificationIdAndUserId(Long notificationId, Long userId) {
        return notificationAnswerMapper.selectByNotificationIdAndUserId(notificationId, userId);
    }

    /**
     * 根据问题ID查询回答列表
     *
     * @param questionId 问题ID
     * @return 回答集合
     */
    @Override
    public List<NotificationAnswer> selectByQuestionId(Long questionId) {
        return notificationAnswerMapper.selectByQuestionId(questionId);
    }

    /**
     * 根据通知ID删除回答
     *
     * @param notificationId 通知ID
     * @return 结果
     */
    @Override
    public int deleteByNotificationId(Long notificationId) {
        // 先查询所有相关回答
        List<NotificationAnswer> answers = notificationAnswerMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NotificationAnswer>()
                .eq(NotificationAnswer::getNotificationId, notificationId)
        );
        
        if (answers != null && !answers.isEmpty()) {
            return notificationAnswerMapper.deleteBatchIds(
                answers.stream().map(NotificationAnswer::getAnswerId).collect(java.util.stream.Collectors.toList())
            );
        }
        return 0;
    }
}