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
     * 根據通知 ID 查詢接收對象列表
     */
    List<NotificationReceiverVO> selectByNotificationId(Long notificationId);

    /**
     * 新增接收對象
     */
    int save(NotificationReceiverSaveDTO notificationReceiverSaveDTO);

    /**
     * 解析接收者列表
     */
    ResolvedReceiversVO resolveReceivers(List<NotificationReceiver> receivers, boolean strictDepartmentCheck);
}
