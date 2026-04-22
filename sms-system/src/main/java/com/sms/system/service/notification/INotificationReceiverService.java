package com.sms.system.service.notification;

import com.sms.system.entity.notification.NotificationReceiver;

import java.util.List;
import java.util.Map;

/**
 * 通知接收对象 Service 接口
 *
 */
public interface INotificationReceiverService {
    /**
     * 根据通知 ID 查询接收对象列表
     *
     * @param notificationId 通知 ID
     * @return 接收对象集合
     */
    List<NotificationReceiver> selectByNotificationId(Long notificationId);
    
    /**
     * 新增接收对象
     *
     * @param receiver 接收对象
     * @return 结果
     */
    int save(NotificationReceiver receiver);

    /**
     * 解析接收者列表，將其轉換為企業微信可識別的 userid 集合
     *
     * @param receivers 原始通告接收者配置列表
     * @return 包含 to_parent_userid、to_student_userid、to_party 的 Map 集合
     */
    Map<String, List<String>> resolveReceivers(List<NotificationReceiver> receivers);
}
