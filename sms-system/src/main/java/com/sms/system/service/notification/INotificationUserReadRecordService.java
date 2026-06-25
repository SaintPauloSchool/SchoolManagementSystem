package com.sms.system.service.notification;

import com.sms.system.entity.notification.NotificationUserReadRecord;
import com.sms.system.entity.vo.ReadStatisticsVO;
import com.sms.system.entity.vo.UnrepliedStudentVO;

import java.util.List;

/**
 * 通知用戶閱讀記錄 Service 接口
 *
 */
public interface INotificationUserReadRecordService {
    /**
     * 批量新增閱讀記錄
     *
     * @param readRecords 閱讀記錄列表
     * @return 結果
     */
    int batchSave(List<NotificationUserReadRecord> readRecords);

    /**
     * 查詢通知閱讀統計信息（強類型 VO）
     *
     * @param notificationId 通知ID
     * @return 閱讀統計 VO
     */
    ReadStatisticsVO getReadStatisticsVO(Long notificationId);

    /**
     * 查詢未回復的學生列表（按學生分組，只要有一個家長回復就算已回復）
     *
     * @param sendRecordId 發送記錄ID
     * @return 未回復學生列表
     */
    List<UnrepliedStudentVO> selectUnrepliedStudents(Long sendRecordId);
    
    /**
     * 查詢發送失敗的閱讀記錄
     *
     * @param sendRecordId 發送記錄ID
     * @return 失敗記錄列表
     */
    List<NotificationUserReadRecord> selectFailedRecords(Long sendRecordId);

    /**
     * 更新閱讀記錄的發送狀態
     *
     * @param readId     閱讀記錄ID
     * @param sendStatus 發送狀態
     */
    void updateSendStatus(Long readId, String sendStatus);
}
