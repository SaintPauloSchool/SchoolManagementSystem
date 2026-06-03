package com.sms.system.service;

import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.entity.vo.ParentStudentMessageInfo;

import java.util.List;

/**
 * 通知消息構建 Service 接口
 * 用於構建個性化的家校通知消息
 */
public interface INotificationMessageService {

    /**
     * 根據家長-學生綁定關系列表，構建完整的消息信息
     * 包括班級名稱和學生姓名
     *
     * @param bindings 家長-學生綁定關系列表
     * @return 家長-學生消息信息列表
     */
    List<ParentStudentMessageInfo> buildMessageInfos(List<SysDepartmentParentBinding> bindings);
}
