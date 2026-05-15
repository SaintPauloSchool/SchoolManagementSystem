package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationUserReadRecord;
import com.sms.system.entity.vo.UserReadRecordVO;
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

    /**
     * 根据发送记录ID查询发送失败的阅读记录列表（send_status = '0'）
     *
     * @param sendRecordId 发送记录ID
     * @return 阅读记录列表
     */
    List<NotificationUserReadRecord> selectFailedBySendRecordId(Long sendRecordId);

    /**
     * 根据发送记录ID查询发送失败的阅读记录VO列表（直接返回VO，用于分页）
     *
     * @param sendRecordId 发送记录ID
     * @return 阅读记录VO列表
     */
    List<UserReadRecordVO> selectFailedBySendRecordIdVO(Long sendRecordId);

    /**
     * 更新阅读记录
     *
     * @param record 阅读记录
     * @return 结果
     */
    int updateById(NotificationUserReadRecord record);
}
