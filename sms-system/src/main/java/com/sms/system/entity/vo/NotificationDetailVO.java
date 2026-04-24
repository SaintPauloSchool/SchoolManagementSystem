package com.sms.system.entity.vo;

import com.sms.system.entity.notification.Notification;
import com.sms.system.entity.notification.NotificationCc;
import com.sms.system.entity.notification.NotificationQuestion;
import com.sms.system.entity.notification.NotificationReceiver;

import java.util.List;

/**
 * 通知詳情 VO
 * <p>
 * 聚合通知基本信息、接收/抄送對象、問題列表以及發送/閱讀統計，
 * 作為 {@code GET /system/notification/{id}} 接口的完整響應體。
 * </p>
 */
public class NotificationDetailVO {

    /** 通知基本信息 */
    private Notification notification;

    /** 接收對象列表 */
    private List<NotificationReceiver> receivers;

    /** 抄送對象列表 */
    private List<NotificationCc> ccs;

    /** 問題列表 */
    private List<NotificationQuestion> questions;

    /** 發送統計（totalCount, successCount, failCount） */
    private SendStatisticsVO sendStatistics;

    /** 閱讀統計（readCount, replyCount） */
    private ReadStatisticsVO readStatistics;

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public Notification getNotification() { return notification; }
    public void setNotification(Notification notification) { this.notification = notification; }

    public List<NotificationReceiver> getReceivers() { return receivers; }
    public void setReceivers(List<NotificationReceiver> receivers) { this.receivers = receivers; }

    public List<NotificationCc> getCcs() { return ccs; }
    public void setCcs(List<NotificationCc> ccs) { this.ccs = ccs; }

    public List<NotificationQuestion> getQuestions() { return questions; }
    public void setQuestions(List<NotificationQuestion> questions) { this.questions = questions; }

    public SendStatisticsVO getSendStatistics() { return sendStatistics; }
    public void setSendStatistics(SendStatisticsVO sendStatistics) { this.sendStatistics = sendStatistics; }

    public ReadStatisticsVO getReadStatistics() { return readStatistics; }
    public void setReadStatistics(ReadStatisticsVO readStatistics) { this.readStatistics = readStatistics; }
}
