package com.sms.system.entity.vo;

import java.util.List;

/**
 * 通知詳情 VO
 * <p>
 * 聚合通知基本資訊、接收/抄送對象、問題列表以及發送/閱讀統計，
 * 作為 {@code GET /system/notification/{id}} 接口的完整響應體。
 * </p>
 */
public class NotificationDetailVO {

    /** 通知基本資訊 */
    private NotificationVO notification;

    /** 接收對象列表 */
    private List<NotificationReceiverVO> receivers;

    /** 抄送對象列表 */
    private List<NotificationCcVO> ccs;

    /** 問題列表 */
    private List<NotificationQuestionVO> questions;

    /** 發送統計（totalCount, successCount, failCount） */
    private SendStatisticsVO sendStatistics;

    /** 閱讀統計（readCount, replyCount） */
    private ReadStatisticsVO readStatistics;

    public NotificationVO getNotification() {
        return notification;
    }

    public void setNotification(NotificationVO notification) {
        this.notification = notification;
    }

    public List<NotificationReceiverVO> getReceivers() {
        return receivers;
    }

    public void setReceivers(List<NotificationReceiverVO> receivers) {
        this.receivers = receivers;
    }

    public List<NotificationCcVO> getCcs() {
        return ccs;
    }

    public void setCcs(List<NotificationCcVO> ccs) {
        this.ccs = ccs;
    }

    public List<NotificationQuestionVO> getQuestions() {
        return questions;
    }

    public void setQuestions(List<NotificationQuestionVO> questions) {
        this.questions = questions;
    }

    public SendStatisticsVO getSendStatistics() {
        return sendStatistics;
    }

    public void setSendStatistics(SendStatisticsVO sendStatistics) {
        this.sendStatistics = sendStatistics;
    }

    public ReadStatisticsVO getReadStatistics() {
        return readStatistics;
    }

    public void setReadStatistics(ReadStatisticsVO readStatistics) {
        this.readStatistics = readStatistics;
    }
}
