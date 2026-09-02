package com.sms.handler.notification;

import com.alibaba.fastjson.JSONObject;
import com.sms.framework.wechat.WechatWorkHttpClient;
import com.sms.system.entity.notification.Notification;
import com.sms.system.entity.notification.NotificationCc;
import com.sms.system.mapper.SysUserRoleMapper;
import com.sms.system.service.notification.INotificationCcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 企業微信應用消息通道：抄送通知與抄送對象撤回。
 */
@Component
public class NotificationCcSendHelper {

    private static final Logger log = LoggerFactory.getLogger(NotificationCcSendHelper.class);
    private static final int DUPLICATE_CHECK_INTERVAL = 1800;

    @Value("${wechat.work.ccNoticeBaseUrl:http://10.32.96.55:8080/cc-notice/}")
    private String ccNoticeBaseUrl;

    @Value("${wechat.work.agentId:1000033}")
    private Integer agentId;

    @Autowired
    private WechatWorkHttpClient wechatWorkHttpClient;

    @Autowired
    private INotificationCcService notificationCcService;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private NotificationMessageContentHelper messageContentHelper;

    /**
     * 解析抄送接收人（含通知配置的抄送對象與系統管理員）。
     */
    public Set<String> resolveCcRecipientUserIds(List<NotificationCc> ccs) {
        Set<String> allUserIds = new HashSet<>();
        if (ccs != null && !ccs.isEmpty()) {
            allUserIds.addAll(notificationCcService.resolveCcUserIds(ccs));
        }
        List<String> userRoleUserIds = sysUserRoleMapper.selectActiveUserIds();
        if (userRoleUserIds != null && !userRoleUserIds.isEmpty()) {
            allUserIds.addAll(userRoleUserIds);
        }
        return allUserIds;
    }

    /**
     * 分批發送抄送文本卡片消息。
     */
    public void sendCcInBatches(Notification notification, List<String> userIds) {
        int totalBatches = NotificationSchoolSendHelper.calcBatchCount(
                userIds.size(), NotificationSchoolSendHelper.PARENT_BATCH_SIZE);

        log.info("通知 {} 的抄送消息需要分 {} 批發送，共 {} 個接收者",
                notification.getNotificationId(), totalBatches, userIds.size());

        for (int i = 0; i < totalBatches; i++) {
            List<String> currentUserIds = NotificationSchoolSendHelper.extractBatch(
                    userIds, i, NotificationSchoolSendHelper.PARENT_BATCH_SIZE);
            if (currentUserIds.isEmpty()) {
                continue;
            }

            JSONObject payload = buildCcTextCardPayload(currentUserIds, notification);
            log.info("發送通知 {} 的抄送消息第 {}/{} 批，接收者: {}",
                    notification.getNotificationId(), i + 1, totalBatches, currentUserIds.size());

            JSONObject result = wechatWorkHttpClient.sendAppMessage(payload);
            Integer errcode = result.getInteger("errcode");
            if (errcode == null || errcode != 0) {
                log.error("通知 {} 抄送消息第 {} 批發送失敗: code={}, msg={}",
                        notification.getNotificationId(), i + 1, errcode, result.getString("errmsg"));
                throw new IllegalStateException("企業微信抄送消息發送失敗（第 " + (i + 1) + " 批）: " + result.toJSONString());
            }
            log.info("通知 {} 抄送消息第 {}/{} 批發送成功", notification.getNotificationId(), i + 1, totalBatches);
        }

        log.info("通知 {} 的抄送消息已全部發送完成，共 {} 批", notification.getNotificationId(), totalBatches);
    }

    /**
     * 分批向抄送對象發送撤回純文本應用消息。
     */
    public void sendRecallTextInBatches(List<String> userIds, String content) {
        int totalBatches = NotificationSchoolSendHelper.calcBatchCount(
                userIds.size(), NotificationSchoolSendHelper.PARENT_BATCH_SIZE);

        for (int i = 0; i < totalBatches; i++) {
            List<String> currentUserIds = NotificationSchoolSendHelper.extractBatch(
                    userIds, i, NotificationSchoolSendHelper.PARENT_BATCH_SIZE);
            if (currentUserIds.isEmpty()) {
                continue;
            }
            try {
                wechatWorkHttpClient.sendAppMessage(buildAppTextPayload(currentUserIds, content));
            } catch (Exception e) {
                log.error("發送撤回微信通知第 {} 批異常 (抄送/管理員)", i + 1, e);
            }
        }
    }

    private JSONObject buildAppTextPayload(List<String> userIds, String content) {
        JSONObject payload = new JSONObject();
        payload.put("touser", String.join("|", userIds));
        payload.put("msgtype", "text");
        payload.put("agentid", agentId);

        JSONObject text = new JSONObject();
        text.put("content", content);
        payload.put("text", text);

        payload.put("safe", 0);
        payload.put("enable_id_trans", 0);
        payload.put("enable_duplicate_check", 0);
        payload.put("duplicate_check_interval", DUPLICATE_CHECK_INTERVAL);
        return payload;
    }

    private JSONObject buildCcTextCardPayload(List<String> userIds, Notification notification) {
        JSONObject payload = new JSONObject();
        payload.put("touser", String.join("|", userIds));
        payload.put("msgtype", "textcard");
        payload.put("agentid", agentId);

        JSONObject textcard = new JSONObject();
        textcard.put("title", "📨 您有一條抄送的通知");

        String title = notification.getTitle() == null ? "" : notification.getTitle().trim();
        String publishTime = messageContentHelper.formatPublishTime(notification.getCreateTime());
        String description = "<div class=\"gray\">⏰ " + publishTime + "</div> "
                + "<div class=\"normal\">📋 " + title + "</div>";
        if (description.length() > 512) {
            description = description.substring(0, 512);
        }
        textcard.put("description", description);

        String state = "wecom_campus_notice_" + notification.getNotificationId();
        String noticeUrl = wechatWorkHttpClient.buildOauthUrl(
                ccNoticeBaseUrl + notification.getNotificationId(), state);
        textcard.put("url", noticeUrl);
        textcard.put("btntxt", "查看詳情");
        payload.put("textcard", textcard);

        payload.put("enable_id_trans", 0);
        payload.put("enable_duplicate_check", 0);
        payload.put("duplicate_check_interval", DUPLICATE_CHECK_INTERVAL);
        return payload;
    }
}
