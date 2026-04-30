package com.sms.system.service.notification;

import javax.servlet.http.HttpServletResponse;

/**
 * 通知导出 Service 接口
 */
public interface INotificationExportService {

    /**
     * 导出通知回复答案（包含统计和详情两个Sheet）
     *
     * @param notificationId 通知ID
     * @param response HTTP响应
     */
    void exportNotificationAnswers(Long notificationId, HttpServletResponse response);
}
