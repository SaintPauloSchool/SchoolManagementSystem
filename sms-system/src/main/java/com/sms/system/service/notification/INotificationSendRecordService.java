package com.sms.system.service.notification;

import com.sms.system.entity.notification.NotificationSendRecord;
import com.sms.system.entity.vo.SendStatisticsVO;

/**
 * 通知发送记录 Service 接口
 *
 */
public interface INotificationSendRecordService {
    /**
     * 新增发送记录
     *
     * @param sendRecord 发送记录
     * @return 结果
     */
    int save(NotificationSendRecord sendRecord);

    /**
     * 查询通知发送统计信息（強類型 VO）
     *
     * @param notificationId 通知ID
     * @return 發送統計 VO
     */
    SendStatisticsVO getSendStatisticsVO(Long notificationId);
}
