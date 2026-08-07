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
     * 根據通知 ID 查詢抄送對象列表。
     */
    List<NotificationCcVO> selectByNotificationId(Long notificationId);

    /**
     * 新增通知抄送記錄（按來源類型各存一行，{@code cc_data} 為成員 ID 數組）。
     */
    int save(NotificationCcSaveDTO notificationCcSaveDTO);

    /**
     * 解析抄送數據，將成員 ID 轉為企業微信 userid 集合（用於實際抄送推送）。
     */
    Set<String> resolveCcUserIds(List<NotificationCc> ccs);

    /**
     * 查詢抄送給指定成員的所有通知 ID（「抄送給我」列表用）。
     */
    Set<Long> selectNotificationIdsByUserId(Long userId);
}
