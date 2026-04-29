package com.sms.system.service.impl.notification;

import com.sms.system.entity.notification.NotificationResendFailRecord;
import com.sms.system.mapper.notification.NotificationResendFailRecordMapper;
import com.sms.system.service.notification.INotificationResendFailRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 通知重发失败记录 Service 业务层处理
 */
@Service
public class NotificationResendFailRecordServiceImpl implements INotificationResendFailRecordService {

    @Autowired
    private NotificationResendFailRecordMapper notificationResendFailRecordMapper;

    @Override
    public void saveOrUpdate(NotificationResendFailRecord record) {
        // 判断是否已存在
        NotificationResendFailRecord existingRecord = notificationResendFailRecordMapper
                .selectByNotificationIdAndUserId(record.getNotificationId(), record.getUserId());

        // 如果不存在，则插入
        if (existingRecord == null) {
            // 新增记录
            record.setFailCount(1);
            record.setStatus("0");
            record.setCreateTime(new Date());
            
            // 将传递过来的 failReason1 和 failMessage1 赋给第1次
            record.setFailReason1(record.getFailReason1());
            record.setFailMessage1(record.getFailMessage1());
            // 新增重发失败记录
            notificationResendFailRecordMapper.insert(record);
        } else {
            // 更新记录
            NotificationResendFailRecord updateObj = getNotificationResendFailRecord(record, existingRecord);
            // 更新重发失败记录（累加失败次数、更新原因）
            notificationResendFailRecordMapper.updateById(updateObj);
        }
    }

    /**
     * 获取更新对象
     *
     * @param record       新的记录
     * @param existingRecord 存在的记录
     * @return 更新对象
     */
    private NotificationResendFailRecord getNotificationResendFailRecord(NotificationResendFailRecord record, NotificationResendFailRecord existingRecord) {
        // 计算失败次数
        int currentFailCount = existingRecord.getFailCount() != null ? existingRecord.getFailCount() : 0;
        int newFailCount = currentFailCount + 1;

        // 設置數據
        NotificationResendFailRecord updateObj = new NotificationResendFailRecord();
        updateObj.setId(existingRecord.getId());
        updateObj.setFailCount(newFailCount);
        updateObj.setUpdateTime(new Date());

        // 根据次数赋值不同的原因字段
        if (newFailCount == 2) {
            updateObj.setFailReason2(record.getFailReason1());
            updateObj.setFailMessage2(record.getFailMessage1());
        } else if (newFailCount == 3) {
            updateObj.setFailReason3(record.getFailReason1());
            updateObj.setFailMessage3(record.getFailMessage1());
        }

        // 判断是否超过最大失败次数或者放弃重发
        if (newFailCount >= MAX_FAIL_COUNT) {
            updateObj.setStatus("1"); // 达到上限，放弃重发
        }
        return updateObj;
    }

    @Override
    public List<NotificationResendFailRecord> selectByNotificationId(Long notificationId) {
        return notificationResendFailRecordMapper.selectByNotificationId(notificationId);
    }

    /**
     * 查询所有放弃重发通知的用户ID
     *
     * @param notificationId 通知ID
     * @return 放弃重发通知的用户ID列表
     */
    @Override
    public Set<String> selectAbandonedUserIds(Long notificationId) {
        // 查询所有失敗的记录
        List<NotificationResendFailRecord> records = notificationResendFailRecordMapper.selectByNotificationId(notificationId);
        Set<String> abandonedIds = new HashSet<>();

        if (records != null) {
            // 遍历记录
            for (NotificationResendFailRecord record : records) {
                // 判断是否超过最大失败次数或者放弃重发
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
