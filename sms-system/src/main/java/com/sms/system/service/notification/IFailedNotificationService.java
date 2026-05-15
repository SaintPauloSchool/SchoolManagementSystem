package com.sms.system.service.notification;

import com.sms.system.entity.vo.FailedNotificationDetailVO;
import com.sms.system.entity.vo.FailedNotificationVO;
import com.sms.system.entity.vo.ResendFailRecordVO;
import com.sms.system.entity.vo.UserReadRecordVO;

import java.util.List;

/**
 * 失败通知 Service 接口
 */
public interface IFailedNotificationService {
    /**
     * 查询失败通知列表
     *
     * @return 失败通知列表
     */
    List<FailedNotificationVO> selectFailedNotificationList();

    /**
     * 根据发送记录ID查询失败通知详情
     *
     * @param sendRecordId 发送记录ID
     * @return 失败通知详情
     */
    FailedNotificationDetailVO selectFailedNotificationDetail(Long sendRecordId);

    /**
     * 分页查询发送失败的用户阅读记录
     *
     * @param sendRecordId 发送记录ID
     * @return 用户阅读记录列表
     */
    List<UserReadRecordVO> selectFailedReadRecordsPage(Long sendRecordId);

    /**
     * 分页查询重发失败记录
     *
     * @param sendRecordId 发送记录ID
     * @return 重发失败记录列表
     */
    List<ResendFailRecordVO> selectResendFailRecordsPage(Long sendRecordId);
}
