package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationResendFailRecord;
import com.sms.system.entity.vo.ResendFailRecordVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知重發失敗記錄 Mapper 接口
 */
public interface NotificationResendFailRecordMapper {

    /**
     * 新增重發失敗記錄
     */
    int insert(NotificationResendFailRecord record);

    /**
     * 更新重發失敗記錄（累加失敗次數、更新原因）
     */
    int updateById(NotificationResendFailRecord record);

    /**
     * 根據通知ID查詢所有重發失敗記錄
     */
    List<NotificationResendFailRecord> selectByNotificationId(@Param("notificationId") Long notificationId);

    /**
     * 根據發送記錄ID查詢重發失敗記錄
     */
    List<NotificationResendFailRecord> selectBySendRecordId(@Param("sendRecordId") Long sendRecordId);

    /**
     * 根據發送記錄ID查詢重發失敗記錄VO列表（直接返回VO，用於分頁）
     */
    List<ResendFailRecordVO> selectBySendRecordIdVO(@Param("sendRecordId") Long sendRecordId);

    /**
     * 根據通知ID和用戶ID查詢單條記錄（用於判斷是否已存在）
     */
    NotificationResendFailRecord selectByNotificationIdAndUserId(
            @Param("notificationId") Long notificationId,
            @Param("userId") String userId);

}
