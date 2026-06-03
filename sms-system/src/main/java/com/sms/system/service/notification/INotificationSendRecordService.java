package com.sms.system.service.notification;

import com.sms.system.entity.notification.NotificationSendRecord;
import com.sms.system.entity.vo.SendStatisticsVO;

import java.util.List;

/**
 * 通知發送記錄 Service 接口
 *
 */
public interface INotificationSendRecordService {
    /**
     * 新增發送記錄
     *
     * @param sendRecord 發送記錄
     * @return 結果
     */
    int save(NotificationSendRecord sendRecord);

    /**
     * 查詢通知發送統計信息（強類型 VO）
     *
     * @param notificationId 通知ID
     * @return 發送統計 VO
     */
    SendStatisticsVO getSendStatisticsVO(Long notificationId);

    /**
     * 根據通知ID查詢發送記錄
     *
     * @param notificationId 通知ID
     * @return 發送記錄
     */
    NotificationSendRecord selectByNotificationId(Long notificationId);
    
    /**
     * 更新發送記錄
     *
     * @param sendRecord 發送記錄
     * @return 結果
     */
    int update(NotificationSendRecord sendRecord);

    /**
     * 查詢所有發送失敗的記錄
     *
     * @return 列表
     */
    List<NotificationSendRecord> selectAllFailedRecords();
}
