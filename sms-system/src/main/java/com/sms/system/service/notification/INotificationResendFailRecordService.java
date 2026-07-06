package com.sms.system.service.notification;

import com.sms.system.entity.notification.NotificationResendFailRecord;

import java.util.List;
import java.util.Set;

/**
 * 通知重發失敗記錄 Service 接口
 */
public interface INotificationResendFailRecordService {

    /** 最大重試次數 */
    int MAX_FAIL_COUNT = 3;

    /**
     * 保存或更新重發失敗記錄
     * - 若該 notificationId + userId 記錄不存在，則新增（fail_count = 1）
     * - 若已存在，則累加 fail_count，並在 fail_count >= MAX_FAIL_COUNT 時將 status 設爲 1（已放棄）
     *
     * @param record 重發失敗記錄
     */
    void saveOrUpdate(NotificationResendFailRecord record);

    /**
     * 根據通知ID查詢所有重發失敗記錄
     *
     * @param notificationId 通知ID
     * @return 記錄列表
     */
    List<NotificationResendFailRecord> selectByNotificationId(Long notificationId);

    /**
     * 根據通知ID構建已放棄用戶ID集合（fail_count >= MAX_FAIL_COUNT 或 status = 1）
     * 用於在重發時快速跳過這些用戶
     *
     * @param notificationId 通知ID
     * @return 已放棄的 userId 集合
     */
    Set<String> selectAbandonedUserIds(Long notificationId);
}
