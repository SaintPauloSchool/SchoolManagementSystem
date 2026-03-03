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
     * 根据通知ID查询问题列表
     *
     * @param notificationId 通知ID
     * @return 问题集合
     */
    @Override
    public List<NotificationQuestion> selectByNotificationId(Long notificationId) {
        return notificationQuestionMapper.selectByNotificationId(notificationId);
    }

    /**
     * 根据父问题ID查询子问题列表
     *
     * @param parentQuestionId 父问题ID
     * @return 子问题集合
     */
    @Override
    public List<NotificationQuestion> selectByParentQuestionId(Long parentQuestionId) {
        return notificationQuestionMapper.selectByParentQuestionId(parentQuestionId);
    }

    /**
     * 根据通知ID删除问题
     *
     * @param notificationId 通知ID
     * @return 结果
     */
    @Override
    public int deleteByNotificationId(Long notificationId) {
        List<NotificationQuestion> questions = selectByNotificationId(notificationId);
        if (questions != null && !questions.isEmpty()) {
            return notificationQuestionMapper.deleteBatchIds(
                questions.stream().map(NotificationQuestion::getQuestionId).collect(java.util.stream.Collectors.toList())
            );
        }
        return 0;
    }
}