package com.sms.system.service.notification;

import com.sms.system.entity.notification.NotificationReceiver;
import com.sms.system.entity.vo.ResolvedReceiversVO;

import java.util.List;

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
     * 解析接收者列表，將其轉換為企業微信可識別的 userid 集合以及精確的家長學生綁定關係
     *
     * @param receivers 原始通告接收者配置列表
     * @return 包含 to_parent_userid、to_student_userid、to_party 和 bindings 的 VO 對象
     */
    ResolvedReceiversVO resolveReceivers(List<NotificationReceiver> receivers);
}
