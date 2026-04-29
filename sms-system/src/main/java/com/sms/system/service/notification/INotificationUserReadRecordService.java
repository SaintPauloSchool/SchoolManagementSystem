package com.sms.system.service.notification;

import com.sms.system.entity.notification.NotificationUserReadRecord;
import com.sms.system.entity.vo.ReadStatisticsVO;
import com.sms.system.entity.vo.UnrepliedStudentVO;

import java.util.List;

/**
 * 通知用户阅读记录 Service 接口
 *
 */
public interface INotificationUserReadRecordService {
    /**
     * 批量新增阅读记录
     *
     * @param readRecords 阅读记录列表
     * @return 结果
     */
    int batchSave(List<NotificationUserReadRecord> readRecords);

    /**
     * 查询通知阅读统计信息（強類型 VO）
     *
     * @param notificationId 通知ID
     * @return 閱讀統計 VO
     */
    ReadStatisticsVO getReadStatisticsVO(Long notificationId);

    /**
     * 查询未回复的学生列表（按学生分组，只要有一个家长回复就算已回复）
     *
     * @param sendRecordId 发送记录ID
     * @return 未回复学生列表
     */
    List<UnrepliedStudentVO> selectUnrepliedStudents(Long sendRecordId);
    
    /**
     * 查询发送失败的阅读记录
     *
     * @param sendRecordId 发送记录ID
     * @return 失败记录列表
     */
    List<NotificationUserReadRecord> selectFailedRecords(Long sendRecordId);

    /**
     * 更新阅读记录的发送状态
     *
     * @param readId 阅读记录ID
     * @param sendStatus 发送状态
     * @return 结果
     */
    int updateSendStatus(Long readId, String sendStatus);
}
