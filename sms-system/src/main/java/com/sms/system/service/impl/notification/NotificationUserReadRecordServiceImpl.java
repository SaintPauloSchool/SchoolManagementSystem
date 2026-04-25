package com.sms.system.service.impl.notification;

import com.sms.system.entity.notification.NotificationSendRecord;
import com.sms.system.entity.notification.NotificationUserReadRecord;
import com.sms.system.entity.vo.ReadStatisticsVO;
import com.sms.system.mapper.notification.NotificationSendRecordMapper;
import com.sms.system.mapper.notification.NotificationUserReadRecordMapper;
import com.sms.system.service.notification.INotificationUserReadRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        // 查詢發送記錄
        NotificationSendRecord sendRecord = notificationSendRecordMapper.selectByNotificationId(notificationId);
        // 沒有發送記錄，返回空
        if (sendRecord == null || sendRecord.getSendRecordId() == null) {
            return new ReadStatisticsVO(0, 0);
        }
        // 查詢閱讀記錄
        List<NotificationUserReadRecord> readRecords =
            notificationUserReadRecordMapper.selectBySendRecordId(sendRecord.getSendRecordId());

        int readCount = 0;
        int replyCount = 0;
        if (readRecords != null) {
            Set<String> readStudents = new HashSet<>();
            Set<String> repliedStudents = new HashSet<>();
            
            for (NotificationUserReadRecord record : readRecords) {
                // 以 studentUserId 爲分組依據，若無則降級使用 userId
                String groupId = record.getStudentUserId();
                if (groupId == null || groupId.trim().isEmpty()) {
                    groupId = record.getUserId();
                }
                
                if ("1".equals(record.getIsRead())) {
                    readStudents.add(groupId);
                }
                if ("1".equals(record.getReplyStatus())) {
                    repliedStudents.add(groupId);
                }
            }
            readCount = readStudents.size();
            replyCount = repliedStudents.size();
        }
        return new ReadStatisticsVO(readCount, replyCount);
    }
}
