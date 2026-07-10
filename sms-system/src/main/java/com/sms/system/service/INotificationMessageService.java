package com.sms.system.service;

import com.sms.system.entity.SysSchoolFamilyContact;
import com.sms.system.entity.notification.receiver.NotificationReceiverTarget;
import com.sms.system.entity.vo.ParentStudentMessageInfo;

import java.util.List;

/**
 * 通知消息構建 Service 接口
 */
public interface INotificationMessageService {

    /**
     * 根據接收目標與家校聯絡人關係，構建個性化消息資訊。
     * <p>{@code sid} 使用 {@code student_id}（學籍 ID），非企微 {@code student_user_id}。</p>
     */
    List<ParentStudentMessageInfo> buildMessageInfos(List<NotificationReceiverTarget> targets,
                                                     List<SysSchoolFamilyContact> relations);
}
