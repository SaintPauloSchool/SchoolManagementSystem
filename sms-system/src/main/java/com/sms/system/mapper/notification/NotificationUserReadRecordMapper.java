package com.sms.system.mapper.notification;

import com.sms.system.entity.notification.NotificationUserReadRecord;
import com.sms.system.entity.vo.UserReadRecordVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知用戶閱讀記錄 Mapper 接口
 *
 */
public interface NotificationUserReadRecordMapper {
    /**
     * 批量新增閱讀記錄
     *
     * @param readRecords 閱讀記錄列表
     * @return 結果
     */
    int batchInsert(@Param("list") List<NotificationUserReadRecord> readRecords);

    /**
     * 根據發送記錄ID查詢閱讀記錄列表
     *
     * @param sendRecordId 發送記錄ID
     * @return 閱讀記錄列表
     */
    List<NotificationUserReadRecord> selectBySendRecordId(Long sendRecordId);

    /**
     * 根據發送記錄ID查詢發送失敗的閱讀記錄列表（send_status = '0'）
     *
     * @param sendRecordId 發送記錄ID
     * @return 閱讀記錄列表
     */
    List<NotificationUserReadRecord> selectFailedBySendRecordId(Long sendRecordId);

    /**
     * 根據發送記錄ID查詢發送失敗的閱讀記錄VO列表（直接返回VO，用於分頁）
     *
     * @param sendRecordId 發送記錄ID
     * @return 閱讀記錄VO列表
     */
    List<UserReadRecordVO> selectFailedBySendRecordIdVO(Long sendRecordId);

    /**
     * 更新閱讀記錄
     *
     * @param record 閱讀記錄
     * @return 結果
     */
    int updateById(NotificationUserReadRecord record);
}
