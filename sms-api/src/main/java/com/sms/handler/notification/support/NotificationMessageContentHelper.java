package com.sms.handler.notification.support;

import com.sms.common.utils.security.Md5Utils;
import com.sms.system.entity.notification.Notification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 通知消息正文輔助類（家校通知、提醒、撤回等純文本內容）。
 */
@Component
public class NotificationMessageContentHelper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${wechat.work.noticeBaseUrl:http://10.32.96.55:8080/notice/}")
    private String noticeBaseUrl;

    @Value("${sms.encryption.salt}")
    private String encryptionSalt;

    public String buildPublishContent(Notification notification) {
        return buildPublishContent(notification, null, null, null);
    }

    public String buildPublishContent(Notification notification, String className,
                                      String studentName, String studentId) {
        String title = notification.getTitle() == null ? "" : notification.getTitle().trim();
        String noticeUrl = buildNoticeUrl(notification.getNotificationId(), studentId);
        String publishTime = formatTime(notification.getCreateTime());

        String header;
        if (className != null && !className.isEmpty() && studentName != null && !studentName.isEmpty()) {
            header = "📢 您有一條 " + className + "-" + studentName + " 新的通告";
        } else {
            header = "📢 您有一條新的通告";
        }

        return header + "\n"
                + "──────────────\n"
                + "📌 標題：\n" + title + "\n\n"
                + "🕒 發佈時間：\n" + publishTime + "\n"
                + "──────────────\n"
                + "👉 請點擊以下連接查看詳情：\n" + noticeUrl;
    }

    public String buildRemindContent(Notification notification) {
        String title = notification.getTitle() == null ? "" : notification.getTitle().trim();
        String noticeUrl = noticeBaseUrl + notification.getNotificationId();
        String replyDeadline = notification.getReplyDeadline() != null
                ? notification.getReplyDeadline().format(DATE_FORMATTER)
                : "";

        return "🔔 溫馨提示\n"
                + "───────────────\n"
                + "您有一條通告需要回覆\n"
                + "───────────────\n"
                + "📌 標題：\n" + title + "\n\n"
                + "⏰ 回復截止時間：\n" + replyDeadline + "\n\n"
                + "👉 請點擊以下連接查看詳情：\n" + noticeUrl;
    }

    public String buildRecallContent(Notification notification) {
        String title = notification.getTitle() == null ? "" : notification.getTitle().trim();
        String recallTime = LocalDateTime.now().format(DATE_FORMATTER);
        return "📢 您有一條通告被撤回\n"
                + "──────────────\n"
                + "📌 標題：\n" + title + "\n\n"
                + "🕒 撤回時間：\n" + recallTime;
    }

    public String formatPublishTime(LocalDateTime createTime) {
        return formatTime(createTime);
    }

    /**
     * 構建通知詳情連結，{@code sid} 為學籍 {@code student_id} 加鹽 MD5。
     */
    private String buildNoticeUrl(Long notificationId, String studentId) {
        if (studentId != null && !studentId.trim().isEmpty()) {
            String encryptedStudentId = Md5Utils.encryptSensitiveId(studentId, encryptionSalt);
            return noticeBaseUrl + notificationId + "?sid=" + encryptedStudentId;
        }
        return noticeBaseUrl + notificationId;
    }

    private String formatTime(LocalDateTime time) {
        return time != null ? time.format(DATE_FORMATTER) : "未知";
    }
}
