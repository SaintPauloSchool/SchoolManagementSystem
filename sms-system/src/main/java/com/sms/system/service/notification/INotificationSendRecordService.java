package com.sms.system.service.notification;

import com.sms.system.entity.notification.NotificationSendRecord;
import com.sms.system.entity.vo.SendStatisticsVO;

import java.util.List;

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

    /**
     * 根据通知ID查询发送记录
     *
     * @param notificationId 通知ID
     * @return 发送记录
     */
    NotificationSendRecord selectByNotificationId(Long notificationId);
    
    /**
     * 更新发送记录
     *
     * @param sendRecord 发送记录
     * @return 结果
     */
    int update(NotificationSendRecord sendRecord);

    /**
     * 查询所有发送失败的记录
     *
     * @return 列表
     */
    List<NotificationSendRecord> selectAllFailedRecords();
}
