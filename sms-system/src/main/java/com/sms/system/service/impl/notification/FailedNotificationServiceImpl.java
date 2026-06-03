package com.sms.system.service.impl.notification;

import com.sms.system.entity.notification.Notification;
import com.sms.system.entity.notification.NotificationSendRecord;
import com.sms.system.entity.vo.FailedNotificationDetailVO;
import com.sms.system.entity.vo.FailedNotificationVO;
import com.sms.system.entity.vo.ResendFailRecordVO;
import com.sms.system.entity.vo.UserReadRecordVO;
import com.sms.system.mapper.notification.NotificationMapper;
import com.sms.system.mapper.notification.NotificationResendFailRecordMapper;
import com.sms.system.mapper.notification.NotificationSendRecordMapper;
import com.sms.system.mapper.notification.NotificationUserReadRecordMapper;
import com.sms.system.service.notification.IFailedNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 失敗通知 Service 業務層處理
 */
@Service
public class FailedNotificationServiceImpl implements IFailedNotificationService {

    @Autowired
    private NotificationSendRecordMapper notificationSendRecordMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private NotificationUserReadRecordMapper notificationUserReadRecordMapper;

    @Autowired
    private NotificationResendFailRecordMapper notificationResendFailRecordMapper;

    /**
     * 查詢失敗通知列表
     *
     * @return 失敗通知列表
     */
    @Override
    public List<FailedNotificationVO> selectFailedNotificationList() {
        // 1. 查詢失敗通知列表
        return notificationSendRecordMapper.selectAllFailedRecordsWithVO();
    }

    /**
     * 根據發送記錄ID查詢失敗通知詳情
     *
     * @param sendRecordId 發送記錄ID
     * @return 失敗通知詳情
     */
    @Override
    public FailedNotificationDetailVO selectFailedNotificationDetail(Long sendRecordId) {
        // 1. 直接查詢發送記錄（不需要分頁）
        NotificationSendRecord sendRecord = notificationSendRecordMapper.selectById(sendRecordId);

        if (sendRecord == null) {
            return null;
        }

        // 2. 查詢對應的通知信息
        Notification notification = notificationMapper.selectById(sendRecord.getNotificationId());
        if (notification == null) {
            return null;
        }

        // 3. 構建詳情VO
        return getFailedNotificationDetailVO(notification, sendRecord);
    }

    /**
     * 構建失敗通知詳情VO
     *
     * @param notification 通知信息
     * @param sendRecord   發送記錄信息
     * @return 詳情VO
     */
    private static FailedNotificationDetailVO getFailedNotificationDetailVO(Notification notification, NotificationSendRecord sendRecord) {
        FailedNotificationDetailVO detailVO = new FailedNotificationDetailVO();
        detailVO.setNotificationId(notification.getNotificationId());
        detailVO.setTitle(notification.getTitle());
        detailVO.setSendRecordId(sendRecord.getSendRecordId());
        detailVO.setSendTime(sendRecord.getSendTime());
        detailVO.setSendStatus(sendRecord.getSendStatus());
        detailVO.setTotalCount(sendRecord.getTotalCount());
        detailVO.setSuccessCount(sendRecord.getSuccessCount());
        detailVO.setFailCount(sendRecord.getFailCount());
        detailVO.setSenderName(sendRecord.getSenderName());
        return detailVO;
    }

    /**
     * 查詢失敗的閱讀記錄
     *
     * @param sendRecordId 發送記錄ID
     * @return 失敗的閱讀記錄
     */
    @Override
    public List<UserReadRecordVO> selectFailedReadRecordsPage(Long sendRecordId) {
        // 查詢失敗的閱讀記錄
        return notificationUserReadRecordMapper.selectFailedBySendRecordIdVO(sendRecordId);
    }

    /**
     * 查詢失敗的重發記錄
     *
     * @param sendRecordId 發送記錄ID
     * @return 失敗的重發記錄
     */
    @Override
    public List<ResendFailRecordVO> selectResendFailRecordsPage(Long sendRecordId) {
        // 查詢失敗的重發記錄
        return notificationResendFailRecordMapper.selectBySendRecordIdVO(sendRecordId);
    }
}
