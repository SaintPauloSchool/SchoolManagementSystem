package com.sms.system.service.notification;

import com.sms.system.entity.dto.NotificationQueryDTO;
import com.sms.system.entity.dto.NotificationSaveDTO;
import com.sms.system.entity.vo.NotificationVO;

import java.util.List;

/**
 * 通知 Service 接口
 */
public interface INotificationService {

    /**
     * 查詢通知列表
     */
    List<NotificationVO> selectNotificationList(NotificationQueryDTO notificationQueryDTO);

    /**
     * 根據 ID 查詢通知
     */
    NotificationVO selectNotificationById(Long notificationId);

    /**
     * 查詢抄送給我的通知列表
     */
    List<NotificationVO> selectCcToMeList(NotificationQueryDTO notificationQueryDTO);

    /**
     * 查詢我發送的通知列表
     */
    List<NotificationVO> selectMySendList(NotificationQueryDTO notificationQueryDTO);

    /**
     * 保存通知（新增或草稿）
     */
    boolean save(NotificationSaveDTO notificationSaveDTO, String createBy);

    /**
     * 撤回通知
     */
    boolean recallNotification(Long notificationId, String updateBy);
}
