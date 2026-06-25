package com.sms.system.service.impl.notification;

import com.sms.system.entity.notification.NotificationSendRecord;
import com.sms.system.entity.vo.SendStatisticsVO;
import com.sms.system.mapper.notification.NotificationSendRecordMapper;
import com.sms.system.service.notification.INotificationSendRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通知發送記錄 Service 業務層處理
 *
 */
@Service
public class NotificationSendRecordServiceImpl implements INotificationSendRecordService {

    @Autowired
    private NotificationSendRecordMapper notificationSendRecordMapper;

    /**
     * 新增發送記錄
     *
     * @param sendRecord 發送記錄
     * @return 結果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int save(NotificationSendRecord sendRecord) {
        return notificationSendRecordMapper.insert(sendRecord);
    }

    /**
     * 查詢通知發送統計信息（強類型 VO）
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

    /**
     * 根據通知ID查詢發送記錄
     *
     * @param notificationId 通知ID
     * @return 發送記錄
     */
    @Override
    public NotificationSendRecord selectByNotificationId(Long notificationId) {
        return notificationSendRecordMapper.selectByNotificationId(notificationId);
    }

    /**
     * 更新發送記錄
     *
     * @param sendRecord 發送記錄
     * @return 結果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(NotificationSendRecord sendRecord) {
        return notificationSendRecordMapper.updateById(sendRecord);
    }

    /**
     * 查詢所有發送失敗的記錄
     *
     * @return 列表
     */
    @Override
    public List<NotificationSendRecord> selectAllFailedRecords() {
        return notificationSendRecordMapper.selectAllFailedRecords();
    }
}
