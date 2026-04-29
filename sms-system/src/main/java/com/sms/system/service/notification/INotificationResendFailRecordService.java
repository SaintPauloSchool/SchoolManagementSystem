package com.sms.system.service.notification;

import com.sms.system.entity.notification.NotificationResendFailRecord;

import java.util.List;

/**
 * 通知重发失败记录 Service 接口
 */
public interface INotificationResendFailRecordService {

    /** 最大重试次数 */
    int MAX_FAIL_COUNT = 3;

    /**
     * 保存或更新重发失败记录
     * - 若该 notificationId + userId 记录不存在，则新增（fail_count = 1）
     * - 若已存在，则累加 fail_count，并在 fail_count >= MAX_FAIL_COUNT 时将 status 设为 1（已放弃）
     *
     * @param record 重发失败记录
     */
    void saveOrUpdate(NotificationResendFailRecord record);

    /**
     * 根据通知ID查询所有重发失败记录
     *
     * @param notificationId 通知ID
     * @return 记录列表
     */
    List<NotificationResendFailRecord> selectByNotificationId(Long notificationId);

    /**
     * 根据通知ID构建已放弃用户ID集合（fail_count >= MAX_FAIL_COUNT 或 status = 1）
     * 用于在重发时快速跳过这些用户
     *
     * @param notificationId 通知ID
     * @return 已放弃的 userId 集合
     */
    java.util.Set<String> selectAbandonedUserIds(Long notificationId);
}
