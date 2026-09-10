package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationQuestion;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知問題 Mapper 接口
 *
 */
public interface NotificationQuestionMapper {
    /**
     * 根據通知 ID 查詢問題列表
     *
     * @param notificationId 通知 ID
     * @return 問題集合
     */
    List<NotificationQuestion> selectByNotificationId(Long notificationId);

    /**
     * 篩選出含有問卷題目的通知 ID
     *
     * @param notificationIds 通知 ID 列表
     * @return 含問卷的通知 ID
     */
    List<Long> selectNotificationIdsHavingQuestions(@Param("notificationIds") List<Long> notificationIds);
    
    /**
     * 新增問題
     *
     * @param question 問題
     * @return 結果
     */
    int insert(NotificationQuestion question);
}
