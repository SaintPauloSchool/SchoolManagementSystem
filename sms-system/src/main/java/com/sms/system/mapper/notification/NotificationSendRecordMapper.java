package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationSendRecord;
import com.sms.system.entity.vo.FailedNotificationVO;

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
     * 根据发送记录ID查询
     *
     * @param sendRecordId 发送记录ID
     * @return 发送记录
     */
    NotificationSendRecord selectById(Long sendRecordId);

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

    /**
     * 查询所有发送失败的记录（关联通知表，返回VO）
     *
     * @return 失败通知VO列表
     */
    List<FailedNotificationVO> selectAllFailedRecordsWithVO();

}
