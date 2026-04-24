package com.sms.system.service.impl.notification;

import com.sms.system.entity.notification.NotificationSendRecord;
import com.sms.system.entity.vo.SendStatisticsVO;
import com.sms.system.mapper.notification.NotificationSendRecordMapper;
import com.sms.system.service.notification.INotificationSendRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 通知发送记录 Service 业务层处理
 *
 */
@Service
public class NotificationSendRecordServiceImpl implements INotificationSendRecordService {

    @Autowired
    private NotificationSendRecordMapper notificationSendRecordMapper;

    /**
     * 新增发送记录
     *
     * @param sendRecord 发送记录
     * @return 结果
     */
    @Override
    public int save(NotificationSendRecord sendRecord) {
        return notificationSendRecordMapper.insert(sendRecord);
    }

    /**
     * 查询通知发送统计信息（強類型 VO）
     *
     * @param notificationId 通知ID
     * @return 發送統計 VO
     */
    @Override
    public SendStatisticsVO getSendStatisticsVO(Long notificationId) {
        NotificationSendRecord sendRecord = notificationSendRecordMapper.selectByNotificationId(notificationId);
        if (sendRecord != null) {
            return new SendStatisticsVO(
                sendRecord.getTotalCount() != null ? sendRecord.getTotalCount() : 0,
                sendRecord.getSuccessCount() != null ? sendRecord.getSuccessCount() : 0,
                sendRecord.getFailCount() != null ? sendRecord.getFailCount() : 0
            );
        }
        return new SendStatisticsVO(0, 0, 0);
    }
}
