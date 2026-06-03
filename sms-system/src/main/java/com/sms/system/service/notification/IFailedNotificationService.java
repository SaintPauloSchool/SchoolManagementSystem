package com.sms.system.service.notification;

import com.sms.system.entity.vo.FailedNotificationDetailVO;
import com.sms.system.entity.vo.FailedNotificationVO;
import com.sms.system.entity.vo.ResendFailRecordVO;
import com.sms.system.entity.vo.UserReadRecordVO;

import java.util.List;

/**
 * 失敗通知 Service 接口
 */
public interface IFailedNotificationService {
    /**
     * 查詢失敗通知列表
     *
     * @return 失敗通知列表
     */
    List<FailedNotificationVO> selectFailedNotificationList();

    /**
     * 根據發送記錄ID查詢失敗通知詳情
     *
     * @param sendRecordId 發送記錄ID
     * @return 失敗通知詳情
     */
    FailedNotificationDetailVO selectFailedNotificationDetail(Long sendRecordId);

    /**
     * 分頁查詢發送失敗的用戶閱讀記錄
     *
     * @param sendRecordId 發送記錄ID
     * @return 用戶閱讀記錄列表
     */
    List<UserReadRecordVO> selectFailedReadRecordsPage(Long sendRecordId);

    /**
     * 分頁查詢重發失敗記錄
     *
     * @param sendRecordId 發送記錄ID
     * @return 重發失敗記錄列表
     */
    List<ResendFailRecordVO> selectResendFailRecordsPage(Long sendRecordId);
}
