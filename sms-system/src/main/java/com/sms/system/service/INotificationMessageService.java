package com.sms.system.service;

import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.entity.vo.ParentStudentMessageInfo;

import java.util.List;

/**
 * 通知消息构建 Service 接口
 * 用于构建个性化的家校通知消息
 */
public interface INotificationMessageService {

    /**
     * 根据家长-学生绑定关系列表，构建完整的消息信息
     * 包括班级名称和学生姓名
     *
     * @param bindings 家长-学生绑定关系列表
     * @return 家长-学生消息信息列表
     */
    List<ParentStudentMessageInfo> buildMessageInfos(List<SysDepartmentParentBinding> bindings);
}
