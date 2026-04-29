package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationSendRecord;

import java.util.List;

/**
 * 通知发送记录 Mapper 接口
 *
 */
public interface NotificationSendRecordMapper {
    /**
     * 新增发送记录
     *
     * @param sendRecord 发送记录
     * @return 结果
     */
    int insert(NotificationSendRecord sendRecord);

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
    int updateById(NotificationSendRecord sendRecord);

    /**
     * 查询所有发送失败的记录
     *
     * @return 列表
     */
    List<NotificationSendRecord> selectAllFailedRecords();

}
