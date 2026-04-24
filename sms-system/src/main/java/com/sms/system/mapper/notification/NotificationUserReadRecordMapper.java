package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationUserReadRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知用户阅读记录 Mapper 接口
 *
 */
public interface NotificationUserReadRecordMapper {
    /**
     * 批量新增阅读记录
     *
     * @param readRecords 阅读记录列表
     * @return 结果
     */
    int batchInsert(@Param("list") List<NotificationUserReadRecord> readRecords);

    /**
     * 根据发送记录ID查询阅读记录列表
     *
     * @param sendRecordId 发送记录ID
     * @return 阅读记录列表
     */
    List<NotificationUserReadRecord> selectBySendRecordId(Long sendRecordId);
}
