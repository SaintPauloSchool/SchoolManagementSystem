package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationSendRecord;
import com.sms.system.entity.vo.FailedNotificationVO;

import java.util.List;

/**
 * 通知發送記錄 Mapper 接口
 *
 */
public interface NotificationSendRecordMapper {
    /**
     * 新增發送記錄
     *
     * @param sendRecord 發送記錄
     * @return 結果
     */
    int insert(NotificationSendRecord sendRecord);

    /**
     * 根據通知ID查詢發送記錄
     *
     * @param notificationId 通知ID
     * @return 發送記錄
     */
    NotificationSendRecord selectByNotificationId(Long notificationId);

    /**
     * 根據發送記錄ID查詢
     *
     * @param sendRecordId 發送記錄ID
     * @return 發送記錄
     */
    NotificationSendRecord selectById(Long sendRecordId);

    /**
     * 更新發送記錄
     *
     * @param sendRecord 發送記錄
     * @return 結果
     */
    int updateById(NotificationSendRecord sendRecord);

    /**
     * 查詢所有發送失敗的記錄
     *
     * @return 列表
     */
    List<NotificationSendRecord> selectAllFailedRecords();

    /**
     * 查詢所有發送失敗的記錄（關聯通知表，返回VO）
     *
     * @return 失敗通知VO列表
     */
    List<FailedNotificationVO> selectAllFailedRecordsWithVO();

}
