package com.sms.handler.system;

import com.alibaba.fastjson.JSONObject;
import com.sms.framework.wechat.WechatWorkHttpClient;
import com.sms.system.entity.task.TaskResult;
import com.sms.system.mapper.SysAdminMapper;
import com.sms.system.service.ISysTaskLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 任務監控處理器
 * 負責處理各類系統任務的監控與警報業務邏輯
 */
@Component("taskMonitorHandler")
public class TaskMonitorHandler {

    private static final Logger log = LoggerFactory.getLogger(TaskMonitorHandler.class);

    @Autowired
    private ISysTaskLogService sysTaskLogService;

    @Autowired
    private SysAdminMapper sysAdminMapper;

    @Autowired
    private WechatWorkHttpClient wechatWorkHttpClient;

    @Value("${wechat.work.agentId:#{null}}")
    private Integer agentId;

    /**
     * 檢查並通知未處理的失敗任務
     *
     * @return 執行結果
     */
    public TaskResult checkAndNotifyFailedTasks() {
        int failedCount = sysTaskLogService.countFailedUnprocessedTasks();
        if (failedCount > 0) {
            log.info("發現未處理的失敗任務數量: {}", failedCount);
            
            // 獲取管理員列表
            List<String> adminUserIds = sysAdminMapper.selectAdminUserIds();
            if (CollectionUtils.isEmpty(adminUserIds)) {
                return TaskResult.fail(0, 1, "發現失敗任務，但找不到可通知的管理員");
            }
            
            String toUser = String.join("|", adminUserIds);
            
            // 組合通知內容
            String content = "您有失敗的定時器任務未處理請查看\n\n目前未處理的失敗任務數量：" + failedCount + "筆，請登入系統日誌進行確認與處理。";
            
            JSONObject message = new JSONObject();
            message.put("touser", toUser);
            message.put("msgtype", "text");
            message.put("agentid", agentId);
            
            JSONObject text = new JSONObject();
            text.put("content", content);
            message.put("text", text);
            
            try {
                JSONObject result = wechatWorkHttpClient.sendAppMessage(message);
                if (result != null && result.getInteger("errcode") == 0) {
                    return TaskResult.success(1, 0, "已發送失敗任務通知給 " + adminUserIds.size() + " 位管理員");
                } else {
                    String errmsg = result != null ? result.getString("errmsg") : "返回結果為空";
                    return TaskResult.fail(0, 1, "發送失敗任務通知失敗: " + errmsg);
                }
            } catch (Exception e) {
                log.error("發送失敗任務通知異常", e);
                return TaskResult.fail(0, 1, "發送失敗任務通知異常: " + e.getMessage());
            }
        }
        
        return TaskResult.success(1, 0, "目前沒有未處理的失敗任務");
    }
}
