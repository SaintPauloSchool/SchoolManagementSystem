package com.sms.system.service.impl.notification;

import com.sms.system.entity.notification.NotificationResendFailRecord;
import com.sms.system.mapper.notification.NotificationResendFailRecordMapper;
import com.sms.system.service.notification.INotificationResendFailRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 通知重發失敗記錄 Service 業務層處理
 */
@Service
public class NotificationResendFailRecordServiceImpl implements INotificationResendFailRecordService {

    @Autowired
    private NotificationResendFailRecordMapper notificationResendFailRecordMapper;

    @Override
    public void saveOrUpdate(NotificationResendFailRecord record) {
        // 判斷是否已存在
        NotificationResendFailRecord existingRecord = notificationResendFailRecordMapper
                .selectByNotificationIdAndUserId(record.getNotificationId(), record.getUserId());

        // 如果不存在，則插入
        if (existingRecord == null) {
            // 新增記錄
            record.setFailCount(1);
            record.setStatus("0");
            record.setCreateTime(LocalDateTime.now());
            
            // 將傳遞過來的 failReason1 和 failMessage1 賦給第1次
            record.setFailReason1(record.getFailReason1());
            record.setFailMessage1(record.getFailMessage1());
            // 新增重發失敗記錄
            notificationResendFailRecordMapper.insert(record);
        } else {
            // 更新記錄
            NotificationResendFailRecord updateObj = getNotificationResendFailRecord(record, existingRecord);
            // 更新重發失敗記錄（累加失敗次數、更新原因）
            notificationResendFailRecordMapper.updateById(updateObj);
        }
    }

    /**
     * 獲取更新對象
     *
     * @param record       新的記錄
     * @param existingRecord 存在的記錄
     * @return 更新對象
     */
    private NotificationResendFailRecord getNotificationResendFailRecord(NotificationResendFailRecord record, NotificationResendFailRecord existingRecord) {
        // 計算失敗次數
        int currentFailCount = existingRecord.getFailCount() != null ? existingRecord.getFailCount() : 0;
        int newFailCount = currentFailCount + 1;

        // 設置數據
        NotificationResendFailRecord updateObj = new NotificationResendFailRecord();
        updateObj.setId(existingRecord.getId());
        updateObj.setFailCount(newFailCount);
        updateObj.setUpdateTime(LocalDateTime.now());

        // 根據次數賦值不同的原因字段
        if (newFailCount == 2) {
            updateObj.setFailReason2(record.getFailReason1());
            updateObj.setFailMessage2(record.getFailMessage1());
        } else if (newFailCount == 3) {
            updateObj.setFailReason3(record.getFailReason1());
            updateObj.setFailMessage3(record.getFailMessage1());
        }

        // 判斷是否超過最大失敗次數或者放棄重發
        if (newFailCount >= MAX_FAIL_COUNT) {
            updateObj.setStatus("1"); // 達到上限，放棄重發
        }
        return updateObj;
    }

    @Override
    public List<NotificationResendFailRecord> selectByNotificationId(Long notificationId) {
        return notificationResendFailRecordMapper.selectByNotificationId(notificationId);
    }

    /**
     * 查詢所有放棄重發通知的用戶ID
     *
     * @param notificationId 通知ID
     * @return 放棄重發通知的用戶ID列表
     */
    @Override
    public Set<String> selectAbandonedUserIds(Long notificationId) {
        // 查詢所有失敗的記錄
        List<NotificationResendFailRecord> records = notificationResendFailRecordMapper.selectByNotificationId(notificationId);
        Set<String> abandonedIds = new HashSet<>();

        if (records != null) {
            // 遍歷記錄
            for (NotificationResendFailRecord record : records) {
                // 判斷是否超過最大失敗次數或者放棄重發
                if (record.getFailCount() != null && record.getFailCount() >= MAX_FAIL_COUNT) {
                    abandonedIds.add(record.getUserId());
                } else if ("1".equals(record.getStatus())) {
                    abandonedIds.add(record.getUserId());
                }
            }
        }
        return abandonedIds;
    }
}
