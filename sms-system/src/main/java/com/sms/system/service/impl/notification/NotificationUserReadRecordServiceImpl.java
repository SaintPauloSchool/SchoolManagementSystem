package com.sms.system.service.impl.notification;

import com.sms.system.entity.notification.NotificationSendRecord;
import com.sms.system.entity.notification.NotificationUserReadRecord;
import com.sms.system.entity.vo.ReadStatisticsVO;
import com.sms.system.entity.vo.UnrepliedStudentVO;
import com.sms.system.mapper.notification.NotificationSendRecordMapper;
import com.sms.system.mapper.notification.NotificationUserReadRecordMapper;
import com.sms.system.service.notification.INotificationUserReadRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * 查询未回复的学生列表（按学生分组，只要有一个家长回复就算已回复）
     *
     * @param sendRecordId 发送记录ID
     * @return 未回复学生列表
     */
    @Override
    public List<UnrepliedStudentVO> selectUnrepliedStudents(Long sendRecordId) {
        // 1. 查询所有阅读记录
        List<NotificationUserReadRecord> allRecords = notificationUserReadRecordMapper.selectBySendRecordId(sendRecordId);
        
        if (allRecords == null || allRecords.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 2. 过滤出有 studentUserId 的记录
        List<NotificationUserReadRecord> validRecords = allRecords.stream()
            .filter(record -> record.getStudentUserId() != null && !record.getStudentUserId().trim().isEmpty())
            .collect(Collectors.toList());
        
        if (validRecords.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 3. 找出已回复的学生和未回复的学生及其对应的家长列表
        // 已回复的学生
        Set<String> repliedStudents = new HashSet<>();
        // 未回复的学生
        Map<String, UnrepliedStudentVO> unrepliedStudentVOMap = new HashMap<>();
        // 家长列表
        for (NotificationUserReadRecord record : validRecords) {
            // 以 studentUserId 爲分組依據，若無則降級使用 userId
            String studentId = record.getStudentUserId();
            
            // 只要有任何一个用户（学生或家长）回复了，该学生就算已回复
            if ("1".equals(record.getReplyStatus())) {
                repliedStudents.add(studentId);
            }
            
            // 只考虑家长类型，收集未回复学生的家长列表
            if ("2".equals(record.getUserType())) {
                // 如果该学生还没有创建 VO，则创建一个新的
                UnrepliedStudentVO vo = unrepliedStudentVOMap.computeIfAbsent(
                    studentId, 
                    k -> new UnrepliedStudentVO(k, new ArrayList<>())
                );
                // 添加家长 ID 到列表中
                vo.getParentUserIds().add(record.getUserId());
            }
        }
        
        // 4. 移除已回复的学生
        repliedStudents.forEach(unrepliedStudentVOMap::remove);
        
        // 5. 构建返回结果
        return new ArrayList<>(unrepliedStudentVOMap.values());
    }

    /**
     * 查询发送失败的阅读记录
     *
     * @param sendRecordId 发送记录ID
     * @return 失败记录列表
     */
    @Override
    public List<NotificationUserReadRecord> selectFailedRecords(Long sendRecordId) {
        List<NotificationUserReadRecord> allRecords = notificationUserReadRecordMapper.selectBySendRecordId(sendRecordId);
        if (allRecords == null || allRecords.isEmpty()) {
            return Collections.emptyList();
        }
        // 发送状态为 0 表示失败
        return allRecords.stream()
                .filter(record -> "0".equals(record.getSendStatus()))
                .collect(Collectors.toList());
    }

    /**
     * 更新阅读记录的发送状态
     *
     * @param readId 阅读记录ID
     * @param sendStatus 发送状态
     * @return 结果
     */
    @Override
    public int updateSendStatus(Long readId, String sendStatus) {
        // 由于没有提供 updateById，我们可以创建一个新的 record 更新它
        NotificationUserReadRecord record = new NotificationUserReadRecord();
        record.setReadId(readId);
        record.setSendStatus(sendStatus);
        return notificationUserReadRecordMapper.updateById(record);
    }
}
