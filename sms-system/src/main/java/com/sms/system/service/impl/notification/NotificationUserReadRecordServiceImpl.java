package com.sms.system.service.impl.notification;

import com.sms.system.entity.notification.NotificationUserReadRecord;
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
}
