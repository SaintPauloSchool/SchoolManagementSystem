package com.sms.system.service.notification;

import javax.servlet.http.HttpServletResponse;

/**
 * 通知導出 Service 接口
 */
public interface INotificationExportService {

    /**
     * 導出通知回復答案（包含統計和詳情兩個Sheet）
     *
     * @param notificationId 通知ID
     * @param response HTTP響應
     */
    void exportNotificationAnswers(Long notificationId, HttpServletResponse response);
}
