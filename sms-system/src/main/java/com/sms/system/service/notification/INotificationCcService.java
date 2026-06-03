package com.sms.system.service.notification;

import com.sms.system.entity.notification.NotificationCc;

import java.util.List;
import java.util.Set;

/**
 * 通知抄送對象 Service 接口
 *
 */
public interface INotificationCcService {
    /**
     * 根據通知 ID 查詢抄送對象列表
     *
     * @param notificationId 通知 ID
     * @return 抄送對象集合
     */
    List<NotificationCc> selectByNotificationId(Long notificationId);
    
    /**
     * 新增抄送對象
     *
     * @param cc 抄送對象
     * @return 結果
     */
    int save(NotificationCc cc);

    /**
     * 解析抄送數據，獲取 userid 列表
     *
     * @param ccs 抄送對象列表
     * @return userid 集合
     */
    Set<String> resolveCcUserIds(List<NotificationCc> ccs);

    /**
     * 根據用戶 ID 和部門 ID 查詢抄送給該用戶的所有通知 ID 列表
     *
     * @param userId 用戶 ID（用於 cc_type=1）
     * @param departmentId 部門 ID（用於 cc_type=2）
     * @return 通知 ID 集合
     */
    Set<Long> selectNotificationIdsByUserId(Long userId, Long departmentId);
}
