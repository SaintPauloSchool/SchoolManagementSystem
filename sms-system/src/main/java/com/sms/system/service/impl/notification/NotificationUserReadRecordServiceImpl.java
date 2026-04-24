package com.sms.system.service.impl.notification;

import com.sms.system.entity.notification.NotificationSendRecord;
import com.sms.system.entity.notification.NotificationUserReadRecord;
import com.sms.system.entity.vo.ReadStatisticsVO;
import com.sms.system.mapper.notification.NotificationSendRecordMapper;
import com.sms.system.mapper.notification.NotificationUserReadRecordMapper;
import com.sms.system.service.notification.INotificationUserReadRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知用户阅读记录 Service 业务层处理
 *
 */
@Service
public class NotificationUserReadRecordServiceImpl implements INotificationUserReadRecordService {

    @Autowired
    private NotificationUserReadRecordMapper notificationUserReadRecordMapper;

    @Autowired
    private NotificationSendRecordMapper notificationSendRecordMapper;

    /**
     * 批量新增阅读记录
     *
     * @param readRecords 阅读记录列表
     * @return 结果
     */
    @Override
    public int batchSave(List<NotificationUserReadRecord> readRecords) {
        if (readRecords == null || readRecords.isEmpty()) {
            return 0;
        }
        return notificationUserReadRecordMapper.batchInsert(readRecords);
    }

    /**
     * 查询通知阅读统计信息（強類型 VO）
     *
     * @param notificationId 通知ID
     * @return 閱讀統計 VO
     */
    @Override
    public ReadStatisticsVO getReadStatisticsVO(Long notificationId) {
        NotificationSendRecord sendRecord = notificationSendRecordMapper.selectByNotificationId(notificationId);
        if (sendRecord == null || sendRecord.getSendRecordId() == null) {
            return new ReadStatisticsVO(0, 0);
        }

        List<NotificationUserReadRecord> readRecords =
            notificationUserReadRecordMapper.selectBySendRecordId(sendRecord.getSendRecordId());

        int readCount = 0;
        int replyCount = 0;
        if (readRecords != null) {
            for (NotificationUserReadRecord record : readRecords) {
                if ("1".equals(record.getIsRead())) {
                    readCount++;
                }
                if ("1".equals(record.getReplyStatus())) {
                    replyCount++;
                }
            }
        }
        return new ReadStatisticsVO(readCount, replyCount);
    }
}
