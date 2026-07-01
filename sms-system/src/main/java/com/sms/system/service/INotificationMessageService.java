package com.sms.system.service;

import com.sms.system.entity.SysSchoolFamilyContact;
import com.sms.system.entity.vo.ParentStudentMessageInfo;

import java.util.List;

/**
 * 通知消息構建 Service 接口
 */
public interface INotificationMessageService {

    /**
     * 根據家長-學生關系列表，構建完整的消息信息
     */
    List<ParentStudentMessageInfo> buildMessageInfos(List<SysSchoolFamilyContact> relations);
}
