package com.sms.web.controller.notification;

import com.sms.common.annotation.Log;
import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.enums.BusinessType;
import com.sms.common.exception.ServiceException;
import com.sms.handler.notification.NotificationPublishHandler;
import com.sms.system.entity.notification.Notification;
import com.sms.system.entity.notification.NotificationReceiver;
import com.sms.system.entity.notification.NotificationCc;
import com.sms.system.entity.notification.NotificationQuestion;
import com.sms.system.entity.vo.*;
import com.sms.system.service.ISysAdminService;
import com.sms.system.service.notification.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    @Autowired
    private INotificationExportService notificationExportService;

    @Autowired
    private IFailedNotificationService failedNotificationService;

    @Autowired
    private ISysAdminService sysAdminService;

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
        // 設置當前登錄用戶信息
        notification.setUserId(getUserId());
        notification.setOpenUserId(getOpenUserId());
        // 查詢列表
        List<Notification> list = notificationService.selectCcToMeList(notification);
        return getDataTable(list);
    }

    /**
     * 查詢我發送的通知列表
     */
    //@PreAuthorize("@ss.hasPermi('system:notification:mySend')")
    @GetMapping("/mySend")
    public TableDataInfo mySend(Notification notification) {
        // 獲取當前登錄用戶的 senderId
        // 設置 senderId 到通知對象中
        notification.setSenderId(getUserId());

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
     * 發佈通知
     */
    // @PreAuthorize("@ss.hasPermi('system:notification:add')")
    @Log(title = "發佈通知", businessType = BusinessType.INSERT)
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult add(@RequestBody Notification notification) {
        // 設置發送人信息（從 Security Context 取得真實登錄用戶）
        notification.setSenderId(getUserId());
        notification.setSenderName(getUsername());
        notification.setCreateTime(LocalDateTime.now());

        if ("1".equals(notification.getStatus()) && notification.getReceivers() != null
                && !notification.getReceivers().isEmpty()) {
            try {
                notificationReceiverService.resolveReceivers(notification.getReceivers(), true);
            } catch (ServiceException e) {
                return AjaxResult.error(e.getMessage());
            }
        }

        // 1. 保存通知基本信息
        if (notificationService.save(notification)) {
            // 獲取生成的通知 ID
            Long notificationId = notification.getNotificationId();
            
            // 2. 保存接收對象
            if (notification.getReceivers() != null && !notification.getReceivers().isEmpty()) {
                for (NotificationReceiver receiver : notification.getReceivers()) {
                    receiver.setNotificationId(notificationId);
                    receiver.setCreateTime(LocalDateTime.now());
                    notificationReceiverService.save(receiver);
                }
            }
            
            // 3. 保存抄送對象
            if (notification.getCcs() != null && !notification.getCcs().isEmpty()) {
                for (NotificationCc cc : notification.getCcs()) {
                    cc.setNotificationId(notificationId);
                    cc.setCreateTime(LocalDateTime.now());
                    notificationCcService.save(cc);
                }
            }
            
            // 4. 保存問題列表
            if (notification.getQuestions() != null && !notification.getQuestions().isEmpty()) {
                int sortOrder = 1;
                for (NotificationQuestion question : notification.getQuestions()) {
                    question.setNotificationId(notificationId);
                    question.setSortOrder(sortOrder++);
                    question.setCreateTime(LocalDateTime.now());
                    notificationQuestionService.save(question);
                }
            }

            // 5. 如果狀態為發佈，則異步發送通知和抄送消息
            if ("1".equals(notification.getStatus())) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        CompletableFuture.runAsync(() -> {
                            try {
                                // 發送通知給接收者
                                notificationPublishHandler.publishToWechat(notification, notification.getReceivers());
                                
                                // 發送抄送消息
                                notificationPublishHandler.sendCcNotifications(notification);
                            } catch (Exception e) {
                                logger.error("異步發送通知失敗: {}", e.getMessage(), e);
                            }
                        });
                    }
                });
            }
            
            return AjaxResult.success();
        } else {
            return AjaxResult.error("發佈通知");
        }
    }

    /**
     * 提示家長回復（重新發送通知給未回復的學生家長）
     */
    //@PreAuthorize("@ss.hasPermi('system:notification:remind')")
    @Log(title = "提示家長回復", businessType = BusinessType.UPDATE)
    @PostMapping("/remindParents/{notificationId}")
    public AjaxResult remindParents(@PathVariable Long notificationId) {
        try {
            Map<String, Object> result = notificationPublishHandler.remindParentsToReply(notificationId);
            
            // 根據 Handler 返回的 success 字段決定返回狀態
            Boolean success = (Boolean) result.get("success");
            if (success != null && success) {
                // 成功或部分成功
                return AjaxResult.success(result);
            } else {
                // 全部失敗，返回 402
                return AjaxResult.error(402, (String) result.get("message"));
            }
        } catch (Exception e) {
            return AjaxResult.error(402, "提示家長回復失敗: " + e.getMessage());
        }
    }
    /**
     * 重新發送失敗通知
     */
    //@PreAuthorize("@ss.hasPermi('system:notification:resend')")
    @Log(title = "重新發送失敗通知", businessType = BusinessType.UPDATE)
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
            return AjaxResult.error(402, "重發失敗通知失敗: " + e.getMessage());
        }
    }

    /**
     * 撤回通知
     */
    //@PreAuthorize("@ss.hasPermi('system:notification:recall')")
    @Log(title = "撤回通知", businessType = BusinessType.UPDATE)
    @PostMapping("/recall/{notificationId}")
    public AjaxResult recall(@PathVariable Long notificationId) {
        try {
            // 1. 查詢原通知對象，用於微信推送中獲取標題等信息
            Notification notification = notificationService.selectNotificationById(notificationId);
            if (notification == null) {
                return AjaxResult.error("通告不存在");
            }
            
            // 2. 執行資料庫更新
            boolean success = notificationService.recallNotification(notificationId);
            if (success) {
                // 3. 異步發送撤回微信通知
                CompletableFuture.runAsync(() -> {
                    try {
                        notificationPublishHandler.sendRecallNotification(notification);
                    } catch (Exception e) {
                        logger.error("發送撤回微信通知失敗: {}", e.getMessage(), e);
                    }
                });
                return AjaxResult.success("撤回成功");
            } else {
                return AjaxResult.error("撤回失敗，通告可能已被撤回或狀態不正確");
            }
        } catch (Exception e) {
            logger.error("撤回通知失敗: {}", e.getMessage(), e);
            return AjaxResult.error("撤回失敗: " + e.getMessage());
        }
    }

    /**
     * 導出通知回復答案（包含統計和詳情兩個Sheet）
     */
    //@PreAuthorize("@ss.hasPermi('system:notification:export')")
    @Log(title = "導出通知回復答案", businessType = BusinessType.EXPORT)
    @GetMapping("/exportAnswers/{notificationId}")
    public void exportAnswers(@PathVariable Long notificationId, HttpServletResponse response) {
        notificationExportService.exportNotificationAnswers(notificationId, response);
    }

    /**
     * 查詢失敗通知列表（需要管理員權限）
     */
    @GetMapping("/failedList")
    public TableDataInfo failedList() {
        // 驗證管理員權限
        String userId = getOpenUserId();
        if (sysAdminService.isNotAdmin(userId)) {
            return getDataTable(new ArrayList<>());
        }

        startPage();
        List<FailedNotificationVO> list = failedNotificationService.selectFailedNotificationList();
        return getDataTable(list);
    }

    /**
     * 查詢失敗通知詳情（需要管理員權限）
     */
    @GetMapping("/failedDetail/{sendRecordId}")
    public AjaxResult failedDetail(@PathVariable Long sendRecordId) {
        // 驗證管理員權限
        String userId = getOpenUserId();
        if (sysAdminService.isNotAdmin(userId)) {
            return AjaxResult.error("無權限訪問");
        }

        FailedNotificationDetailVO detail = failedNotificationService.selectFailedNotificationDetail(sendRecordId);
        if (detail == null) {
            return AjaxResult.error("未找到相關數據");
        }

        return AjaxResult.success(detail);
    }

    /**
     * 分頁查詢發送失敗的用戶閱讀記錄（需要管理員權限）
     */
    @GetMapping("/failedReadRecords/{sendRecordId}")
    public TableDataInfo failedReadRecords(@PathVariable Long sendRecordId) {
        // 驗證管理員權限
        String userId = getOpenUserId();
        if (sysAdminService.isNotAdmin(userId)) {
            return getDataTable(new ArrayList<>());
        }

        startPage();
        List<UserReadRecordVO> list =
            failedNotificationService.selectFailedReadRecordsPage(sendRecordId);
        return getDataTable(list);
    }

    /**
     * 分頁查詢重發失敗記錄（需要管理員權限）
     */
    @GetMapping("/resendFailRecords/{sendRecordId}")
    public TableDataInfo resendFailRecords(@PathVariable Long sendRecordId) {
        // 驗證管理員權限
        String userId = getOpenUserId();
        if (sysAdminService.isNotAdmin(userId)) {
            return getDataTable(new ArrayList<>());
        }

        startPage();
        List<ResendFailRecordVO> list =
            failedNotificationService.selectResendFailRecordsPage(sendRecordId);
        return getDataTable(list);
    }
}
