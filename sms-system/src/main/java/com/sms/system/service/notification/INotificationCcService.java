package com.sms.system.service.notification;

import com.sms.system.entity.dto.NotificationCcSaveDTO;
import com.sms.system.entity.notification.NotificationCc;
import com.sms.system.entity.vo.NotificationCcVO;

import java.util.List;
import java.util.Set;

/**
 * 通知抄送對象 Service 接口
 */
public interface INotificationCcService {
    /**
     * 根據通知 ID 查詢抄送對象列表
     */
    List<NotificationCcVO> selectByNotificationId(Long notificationId);

    /**
     * 新增抄送對象
     */
    int save(NotificationCcSaveDTO notificationCcSaveDTO);

    /**
     * 解析抄送數據，獲取 userid 列表
     */
    Set<String> resolveCcUserIds(List<NotificationCc> ccs);

    /**
     * 根據用戶 ID 和部門 ID 查詢抄送給該用戶的所有通知 ID 列表
     */
    Set<Long> selectNotificationIdsByUserId(Long userId, Long departmentId);
}
