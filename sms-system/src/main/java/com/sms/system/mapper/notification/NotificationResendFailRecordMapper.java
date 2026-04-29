package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationResendFailRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知重发失败记录 Mapper 接口
 */
public interface NotificationResendFailRecordMapper {

    /**
     * 新增重发失败记录
     */
    int insert(NotificationResendFailRecord record);

    /**
     * 更新重发失败记录（累加失败次数、更新原因）
     */
    int updateById(NotificationResendFailRecord record);

    /**
     * 根据通知ID查询所有重发失败记录
     */
    List<NotificationResendFailRecord> selectByNotificationId(@Param("notificationId") Long notificationId);

    /**
     * 根据通知ID和用户ID查询单条记录（用于判断是否已存在）
     */
    NotificationResendFailRecord selectByNotificationIdAndUserId(
            @Param("notificationId") Long notificationId,
            @Param("userId") String userId);

}
