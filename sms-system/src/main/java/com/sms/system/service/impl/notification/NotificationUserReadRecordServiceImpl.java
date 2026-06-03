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
 * 通知用戶閱讀記錄 Service 業務層處理
 *
 */
@Service
public class NotificationUserReadRecordServiceImpl implements INotificationUserReadRecordService {

    @Autowired
    private NotificationUserReadRecordMapper notificationUserReadRecordMapper;

    @Autowired
    private NotificationSendRecordMapper notificationSendRecordMapper;

    /**
     * 批量新增閱讀記錄
     *
     * @param readRecords 閱讀記錄列表
     * @return 結果
     */
    @Override
    public int batchSave(List<NotificationUserReadRecord> readRecords) {
        if (readRecords == null || readRecords.isEmpty()) {
            return 0;
        }
        return notificationUserReadRecordMapper.batchInsert(readRecords);
    }

    /**
     * 查詢通知閱讀統計信息（強類型 VO）
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
     * 查詢未回復的學生列表（按學生分組，只要有一個家長回復就算已回復）
     *
     * @param sendRecordId 發送記錄ID
     * @return 未回復學生列表
     */
    @Override
    public List<UnrepliedStudentVO> selectUnrepliedStudents(Long sendRecordId) {
        // 1. 查詢所有閱讀記錄
        List<NotificationUserReadRecord> allRecords = notificationUserReadRecordMapper.selectBySendRecordId(sendRecordId);
        
        if (allRecords == null || allRecords.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 2. 過濾出有 studentUserId 的記錄
        List<NotificationUserReadRecord> validRecords = allRecords.stream()
            .filter(record -> record.getStudentUserId() != null && !record.getStudentUserId().trim().isEmpty())
            .collect(Collectors.toList());
        
        if (validRecords.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 3. 找出已回復的學生和未回復的學生及其對應的家長列表
        // 已回復的學生
        Set<String> repliedStudents = new HashSet<>();
        // 未回復的學生
        Map<String, UnrepliedStudentVO> unrepliedStudentVOMap = new HashMap<>();
        // 家長列表
        for (NotificationUserReadRecord record : validRecords) {
            // 以 studentUserId 爲分組依據，若無則降級使用 userId
            String studentId = record.getStudentUserId();
            
            // 只要有任何一個用戶（學生或家長）回復了，該學生就算已回復
            if ("1".equals(record.getReplyStatus())) {
                repliedStudents.add(studentId);
            }
            
            // 只考慮家長類型，收集未回復學生的家長列表
            if ("2".equals(record.getUserType())) {
                // 如果該學生還沒有創建 VO，則創建一個新的
                UnrepliedStudentVO vo = unrepliedStudentVOMap.computeIfAbsent(
                    studentId, 
                    k -> new UnrepliedStudentVO(k, new ArrayList<>())
                );
                // 添加家長 ID 到列表中
                vo.getParentUserIds().add(record.getUserId());
            }
        }
        
        // 4. 移除已回復的學生
        repliedStudents.forEach(unrepliedStudentVOMap::remove);
        
        // 5. 構建返回結果
        return new ArrayList<>(unrepliedStudentVOMap.values());
    }

    /**
     * 查詢發送失敗的閱讀記錄
     *
     * @param sendRecordId 發送記錄ID
     * @return 失敗記錄列表
     */
    @Override
    public List<NotificationUserReadRecord> selectFailedRecords(Long sendRecordId) {
        List<NotificationUserReadRecord> allRecords = notificationUserReadRecordMapper.selectBySendRecordId(sendRecordId);
        if (allRecords == null || allRecords.isEmpty()) {
            return Collections.emptyList();
        }
        // 發送狀態爲 0 表示失敗
        return allRecords.stream()
                .filter(record -> "0".equals(record.getSendStatus()))
                .collect(Collectors.toList());
    }

    /**
     * 更新閱讀記錄的發送狀態
     *
     * @param readId 閱讀記錄ID
     * @param sendStatus 發送狀態
     * @return 結果
     */
    @Override
    public int updateSendStatus(Long readId, String sendStatus) {
        // 由於沒有提供 updateById，我們可以創建一個新的 record 更新它
        NotificationUserReadRecord record = new NotificationUserReadRecord();
        record.setReadId(readId);
        record.setSendStatus(sendStatus);
        return notificationUserReadRecordMapper.updateById(record);
    }
}
