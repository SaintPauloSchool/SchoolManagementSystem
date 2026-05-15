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
 * 失败通知 Service 业务层处理
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
     * 查询失败通知列表
     *
     * @return 失败通知列表
     */
    @Override
    public List<FailedNotificationVO> selectFailedNotificationList() {
        // 1. 查询失败通知列表
        return notificationSendRecordMapper.selectAllFailedRecordsWithVO();
    }

    /**
     * 根据发送记录ID查询失败通知详情
     *
     * @param sendRecordId 发送记录ID
     * @return 失败通知详情
     */
    @Override
    public FailedNotificationDetailVO selectFailedNotificationDetail(Long sendRecordId) {
        // 1. 直接查询发送记录（不需要分页）
        NotificationSendRecord sendRecord = notificationSendRecordMapper.selectById(sendRecordId);

        if (sendRecord == null) {
            return null;
        }

        // 2. 查询对应的通知信息
        Notification notification = notificationMapper.selectById(sendRecord.getNotificationId());
        if (notification == null) {
            return null;
        }

        // 3. 构建详情VO
        return getFailedNotificationDetailVO(notification, sendRecord);
    }

    /**
     * 构建失败通知详情VO
     *
     * @param notification 通知信息
     * @param sendRecord   发送记录信息
     * @return 详情VO
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
     * 查询失败的阅读记录
     *
     * @param sendRecordId 发送记录ID
     * @return 失败的阅读记录
     */
    @Override
    public List<UserReadRecordVO> selectFailedReadRecordsPage(Long sendRecordId) {
        // 查询失败的阅读记录
        return notificationUserReadRecordMapper.selectFailedBySendRecordIdVO(sendRecordId);
    }

    /**
     * 查询失败的重发记录
     *
     * @param sendRecordId 发送记录ID
     * @return 失败的重发记录
     */
    @Override
    public List<ResendFailRecordVO> selectResendFailRecordsPage(Long sendRecordId) {
        // 查询失败的重发记录
        return notificationResendFailRecordMapper.selectBySendRecordIdVO(sendRecordId);
    }
}
