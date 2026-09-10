package com.sms.system.service.notification;

import com.sms.system.entity.dto.NotificationQuestionSaveDTO;
import com.sms.system.entity.vo.NotificationQuestionVO;

import java.util.List;

/**
 * 通知問題 Service 接口
 */
public interface INotificationQuestionService {
    /**
     * 根據通知 ID 查詢問題列表
     */
    List<NotificationQuestionVO> selectByNotificationId(Long notificationId);

    /**
     * 篩選出含有問卷題目的通知 ID
     */
    List<Long> selectNotificationIdsHavingQuestions(List<Long> notificationIds);

    /**
     * 新增問題
     */
    int save(NotificationQuestionSaveDTO notificationQuestionSaveDTO);
}
