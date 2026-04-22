package com.sms.handler.notification;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.framework.wechat.WechatWorkHttpClient;
import com.sms.system.entity.notification.Notification;
import com.sms.system.entity.notification.NotificationReceiver;
import com.sms.system.service.notification.INotificationReceiverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 通告發佈處理器
 * 負責將通告消息推送給指定的接收者（如通過企業微信發送家校通知）
 */
@Component
public class NotificationPublishHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublishHandler.class);

    /**
     * 默認的通告查看基礎 URL。如果通告本身沒有跳轉鏈接，將使用此基礎 URL 拼接通告 ID。
     */
    @Value("${wechat.work.noticeBaseUrl:http://10.32.96.55:8080/notice/}")
    private String noticeBaseUrl;

    @Value("${wechat.work.agentId:#{null}}")
    private Integer agentId;

    @Autowired
    private INotificationReceiverService notificationReceiverService;

    @Autowired
    private WechatWorkHttpClient wechatWorkHttpClient;

    /**
     * 將通告發佈到企業微信（家校通信/外部聯繫人消息）
     *
     * @param notification 通告實體對象
     * @param receivers    通告的接收者列表設置
     * @throws IllegalStateException 當沒有解析出有效的接收者，或微信發送失敗時拋出異常
     */
    public void publishToWechat(Notification notification, List<NotificationReceiver> receivers) {
        // 1. 解析接收者，提取家長、學生、部門的 ID 列表
        Map<String, List<String>> resolvedReceivers = notificationReceiverService.resolveReceivers(receivers);
        List<String> parentUserIds = resolvedReceivers.getOrDefault("to_parent_userid", Collections.emptyList());
        List<String> studentUserIds = resolvedReceivers.getOrDefault("to_student_userid", Collections.emptyList());
        List<String> partyIds = resolvedReceivers.getOrDefault("to_party", Collections.emptyList());

        // 如果沒有任何接收者，則拋出異常避免無效調用
        if (parentUserIds.isEmpty() && studentUserIds.isEmpty() && partyIds.isEmpty()) {
            log.warn("未找到有效的微信收件人進行通知 {}", notification.getNotificationId());
            throw new IllegalStateException("未解析出有效的微信接收者");
        }

        // 2. 構建微信家校消息(家校通知)的請求 Payload
        JSONObject payload = buildWechatPayload(parentUserIds, studentUserIds, partyIds, notification);

        // 3. 調用企業微信客戶端發送消息
        log.info("發布通知 {} 到微信，有效載荷：{}", notification.getNotificationId(), payload.toJSONString());
        JSONObject result = wechatWorkHttpClient.sendSchoolNotification(payload);
        
        Integer errcode = result.getInteger("errcode");
        if (errcode == null || errcode != 0) {
            log.error("Wechat notification publish failed: code={}, msg={}", errcode, result.getString("errmsg"));
            throw new IllegalStateException("企業微信消息發送失敗: " + result.toJSONString());
        }
        
        log.info("通知 {} 已成功發佈到微信", notification.getNotificationId());
    }

    /**
     * 構建發送給企業微信接口的 JSON 數據實體
     */
    private JSONObject buildWechatPayload(List<String> parentUserIds, List<String> studentUserIds, List<String> partyIds, Notification notification) {
        JSONObject payload = new JSONObject();
        // recv_scope: 0表示發送給指定的家長、學生或部門
        payload.put("recv_scope", 0);
        
        // 設置接收者，修復了原代碼中忽視 studentUserIds 和 partyIds 的邏輯
        payload.put("to_parent_userid", toJsonArray(parentUserIds));
        payload.put("to_student_userid", toJsonArray(studentUserIds));
        payload.put("to_party", toJsonArray(partyIds));
        
        // toall: 0表示不發送給所有人
        payload.put("toall", 0);
        
        // 消息類型與應用ID
        payload.put("msgtype", "text");
        payload.put("agentid", agentId);

        // 構建文本內容
        JSONObject text = new JSONObject();
        text.put("content", buildContent(notification));
        payload.put("text", text);

        // 其他發送配置
        payload.put("enable_id_trans", 0);
        payload.put("enable_duplicate_check", 0);
        payload.put("duplicate_check_interval", 1800); // 防重覆發送校驗時間，默認1800秒
        
        return payload;
    }

    /**
     * 構建消息文本內容
     *
     * @param notification 通告實體
     * @return 格式化後的文本內容
     */
    private String buildContent(Notification notification) {
        String title = notification.getTitle() == null ? "" : notification.getTitle().trim();
        String noticeUrl = notification.getJumpUrl();
        
        // 如果通告沒有自定義的跳轉鏈接，則使用默認的詳情頁鏈接
        if (noticeUrl == null || noticeUrl.trim().isEmpty()) {
            noticeUrl = noticeBaseUrl + notification.getNotificationId();
        }
        
        return "您有一條新的通告\n\n"
            + "標題：" + title + "\n\n"
            + "請點擊以下連接查看詳情：\n" + noticeUrl;
    }

    /**
     * 輔助方法：將 List 轉換為 JSON 數組
     */
    private JSONArray toJsonArray(List<String> values) {
        JSONArray array = new JSONArray();
        if (values != null && !values.isEmpty()) {
            array.addAll(values);
        }
        return array;
    }
}
