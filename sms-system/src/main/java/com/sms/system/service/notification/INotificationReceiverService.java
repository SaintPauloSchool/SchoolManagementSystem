package com.sms.system.service.notification;

import com.sms.system.entity.dto.NotificationReceiverSaveDTO;
import com.sms.system.entity.notification.NotificationReceiver;
import com.sms.system.entity.vo.NotificationReceiverVO;
import com.sms.system.entity.vo.ResolvedReceiversVO;

import java.util.List;

/**
 * 通知接收對象 Service 接口
 */
public interface INotificationReceiverService {
    /**
     * 根據通知 ID 查詢接收對象列表。
     */
    List<NotificationReceiverVO> selectByNotificationId(Long notificationId);

    /**
     * 新增通知接收記錄（按來源類型各存一行，{@code receive_data} 為 parentUserId 數組）。
     */
    int save(NotificationReceiverSaveDTO notificationReceiverSaveDTO);

    /**
     * 解析接收對象列表，將 {@code receive_data} 中的 parentUserId 轉為實際發送目標。
     *
     * @param strictDepartmentCheck 發佈時建議 {@code true}，定時任務可放寬為 {@code false}
     */
    ResolvedReceiversVO resolveReceivers(List<NotificationReceiver> receivers, boolean strictDepartmentCheck);
}
