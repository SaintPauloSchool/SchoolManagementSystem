package com.sms.web.controller.notification;

import com.sms.common.annotation.Log;
import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.enums.BusinessType;
import com.sms.handler.notification.NotificationPublishHandler;
import com.sms.system.entity.notification.Notification;
import com.sms.system.entity.notification.NotificationReceiver;
import com.sms.system.entity.notification.NotificationCc;
import com.sms.system.entity.notification.NotificationQuestion;
import com.sms.system.entity.vo.NotificationDetailVO;
import com.sms.system.service.notification.INotificationService;
import com.sms.system.service.notification.INotificationReceiverService;
import com.sms.system.service.notification.INotificationCcService;
import com.sms.system.service.notification.INotificationQuestionService;
import com.sms.system.service.notification.INotificationSendRecordService;
import com.sms.system.service.notification.INotificationUserReadRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 通知 Controller
 *
 */
@RestController
@RequestMapping("/system/notification")
public class NotificationController extends BaseController {

    @Autowired
    private INotificationService notificationService;

    @Autowired
    private INotificationReceiverService notificationReceiverService;

    @Autowired
    private INotificationCcService notificationCcService;

    @Autowired
    private INotificationQuestionService notificationQuestionService;

    @Autowired
    private NotificationPublishHandler notificationPublishHandler;

    @Autowired
    private INotificationSendRecordService notificationSendRecordService;

    @Autowired
    private INotificationUserReadRecordService notificationUserReadRecordService;

    /**
     * 查詢通知列表
     */
    //@PreAuthorize("@ss.hasPermi('system:notification:list')")
    @GetMapping("/list")
    public TableDataInfo list(Notification notification) {
        startPage();
        List<Notification> list = notificationService.selectNotificationList(notification);
        return getDataTable(list);
    }

    /**
     * 查詢抄送給我的通知列表
     */
    //@PreAuthorize("@ss.hasPermi('system:notification:ccToList')")
    @GetMapping("/ccToMe")
    public TableDataInfo ccToMe(Notification notification) {
        // 獲取當前登錄用戶信息
        Long userId = 1L;
        String userType = getUserType();
        
        // 設置 userId 和 userType 到通知對象中
        notification.setUserId(userId);
        notification.setUserType(userType);
        
        startPage();
        List<Notification> list = notificationService.selectCcToMeList(notification);
        return getDataTable(list);
    }

    /**
     * 查詢我發送的通知列表
     */
    //@PreAuthorize("@ss.hasPermi('system:notification:mySend')")
    @GetMapping("/mySend")
    public TableDataInfo mySend(Notification notification) {
        Long senderId = 1L;
        
        // 設置 senderId 到通知對象中
        notification.setSenderId(senderId);
        
        startPage();
        List<Notification> list = notificationService.selectMySendList(notification);
        return getDataTable(list);
    }

    /**
     * 獲取通知詳細信息
     */
    //@PreAuthorize("@ss.hasPermi('system:notification:query')")
    @GetMapping(value = "/{notificationId}")
    public AjaxResult getInfo(@PathVariable("notificationId") Long notificationId) {
        Notification notification = notificationService.selectNotificationById(notificationId);
        if (notification == null) {
            return AjaxResult.error("通知不存在");
        }

        NotificationDetailVO vo = new NotificationDetailVO();
        vo.setNotification(notification);
        vo.setReceivers(notificationReceiverService.selectByNotificationId(notificationId));
        vo.setCcs(notificationCcService.selectByNotificationId(notificationId));
        vo.setQuestions(notificationQuestionService.selectByNotificationId(notificationId));
        vo.setSendStatistics(notificationSendRecordService.getSendStatisticsVO(notificationId));
        vo.setReadStatistics(notificationUserReadRecordService.getReadStatisticsVO(notificationId));

        return AjaxResult.success(vo);
    }

    /**
     * 發布通知
     */
    // @PreAuthorize("@ss.hasPermi('system:notification:add')")
    @Log(title = "發布通知", businessType = BusinessType.INSERT)
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult add(@RequestBody Notification notification) {
        // 設置發送人信息（使用假數據）
        notification.setSenderId(1L);
        notification.setSenderName("測試用戶");
        notification.setCreateTime(new Date());
        
        // 1. 保存通知基本信息
        if (notificationService.save(notification)) {
            // 獲取生成的通知 ID
            Long notificationId = notification.getNotificationId();
            
            // 2. 保存接收對象
            if (notification.getReceivers() != null && !notification.getReceivers().isEmpty()) {
                for (NotificationReceiver receiver : notification.getReceivers()) {
                    receiver.setNotificationId(notificationId);
                    receiver.setCreateTime(new Date());
                    notificationReceiverService.save(receiver);
                }
            }
            
            // 3. 保存抄送對象
            if (notification.getCcs() != null && !notification.getCcs().isEmpty()) {
                for (NotificationCc cc : notification.getCcs()) {
                    cc.setNotificationId(notificationId);
                    cc.setCreateTime(new Date());
                    notificationCcService.save(cc);
                }
            }
            
            // 4. 保存問題列表
            if (notification.getQuestions() != null && !notification.getQuestions().isEmpty()) {
                int sortOrder = 1;
                for (NotificationQuestion question : notification.getQuestions()) {
                    question.setNotificationId(notificationId);
                    question.setSortOrder(sortOrder++);
                    question.setCreateTime(new Date());
                    notificationQuestionService.save(question);
                }
            }

            // 5. 如果狀態為發布，則發送通知和抄送消息
            if ("1".equals(notification.getStatus())) {
                // 發送通知給接收者
                notificationPublishHandler.publishToWechat(notification, notification.getReceivers());
                
                // 發送抄送消息
                notificationPublishHandler.sendCcNotifications(notification);
            }
            
            return AjaxResult.success();
        } else {
            return AjaxResult.error("發布失敗");
        }
    }

    /**
     * 修改通知
     */
    //@PreAuthorize("@ss.hasPermi('system:notification:edit')")
    @Log(title = "修改通知", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Notification notification) {
        int result = notificationService.updateNotification(notification);
        return result > 0 ? AjaxResult.success() : AjaxResult.error("修改失敗");
    }

    /**
     * 刪除通知
     */
    //@PreAuthorize("@ss.hasPermi('system:notification:remove')")
    @Log(title = "刪除通知", businessType = BusinessType.DELETE)
    @DeleteMapping("/{notificationIds}")
    public AjaxResult remove(@PathVariable Long[] notificationIds) {
        int result = notificationService.deleteNotificationByIds(Arrays.asList(notificationIds));
        return result > 0 ? AjaxResult.success() : AjaxResult.error("刪除失敗");
    }

    /**
     * 撤回通知
     */
    //@PreAuthorize("@ss.hasPermi('system:notification:withdraw')")
    @Log(title = "撤回通知", businessType = BusinessType.UPDATE)
    @PutMapping("/withdraw/{notificationId}")
    public AjaxResult withdraw(@PathVariable Long notificationId) {
        Notification notification = notificationService.selectNotificationById(notificationId);
        if (notification == null) {
            return AjaxResult.error("通知不存在");
        }
        
        notification.setStatus("2"); // 設置為已撤回狀態
        int result = notificationService.updateNotification(notification);
        return result > 0 ? AjaxResult.success() : AjaxResult.error("撤回失敗");
    }

    /**
     * 提示家长回复（重新发送通知给未回复的学生家长）
     */
    //@PreAuthorize("@ss.hasPermi('system:notification:remind')")
    @Log(title = "提示家长回复", businessType = BusinessType.UPDATE)
    @PostMapping("/remindParents/{notificationId}")
    public AjaxResult remindParents(@PathVariable Long notificationId) {
        try {
            Map<String, Object> result = notificationPublishHandler.remindParentsToReply(notificationId);
            
            // 根据 Handler 返回的 success 字段决定返回状态
            Boolean success = (Boolean) result.get("success");
            if (success != null && success) {
                // 成功或部分成功
                return AjaxResult.success(result);
            } else {
                // 全部失败，返回 402
                return AjaxResult.error(402, (String) result.get("message"));
            }
        } catch (Exception e) {
            return AjaxResult.error(402, "提示家长回复失败: " + e.getMessage());
        }
    }
    /**
     * 重新发送失败通知
     */
    //@PreAuthorize("@ss.hasPermi('system:notification:resend')")
    @Log(title = "重新发送失败通知", businessType = BusinessType.UPDATE)
    @PostMapping("/resendFailed/{notificationId}")
    public AjaxResult resendFailed(@PathVariable Long notificationId) {
        try {
            Map<String, Object> result = notificationPublishHandler.resendFailedNotifications(notificationId, false);
            Boolean success = (Boolean) result.get("success");
            if (success != null && success) {
                return AjaxResult.success(result);
            } else {
                return AjaxResult.error(402, (String) result.get("message"));
            }
        } catch (Exception e) {
            return AjaxResult.error(402, "重发失败通知失败: " + e.getMessage());
        }
    }
}
