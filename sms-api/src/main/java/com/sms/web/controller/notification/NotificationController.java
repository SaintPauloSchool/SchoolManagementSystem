package com.sms.web.controller.notification;

import com.sms.common.annotation.Log;
import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.enums.BusinessType;
import com.sms.common.exception.ServiceException;
import com.sms.handler.notification.NotificationPublishHandler;
import com.sms.system.entity.dto.NotificationQueryDTO;
import com.sms.system.entity.dto.NotificationSaveDTO;
import com.sms.system.entity.vo.FailedNotificationDetailVO;
import com.sms.system.entity.vo.FailedNotificationVO;
import com.sms.system.entity.vo.NotificationDetailVO;
import com.sms.system.entity.vo.NotificationVO;
import com.sms.system.entity.vo.ResendFailRecordVO;
import com.sms.system.entity.vo.UserReadRecordVO;
import com.sms.system.service.ISysUserRoleService;
import com.sms.system.service.notification.IFailedNotificationService;
import com.sms.system.service.notification.INotificationCcService;
import com.sms.system.service.notification.INotificationExportService;
import com.sms.system.service.notification.INotificationQuestionService;
import com.sms.system.service.notification.INotificationReceiverService;
import com.sms.system.service.notification.INotificationSendRecordService;
import com.sms.system.service.notification.INotificationService;
import com.sms.system.service.notification.INotificationUserReadRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 通知 Controller
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
    private ISysUserRoleService sysUserRoleService;

    @Log(title = "查詢通知列表", businessType = BusinessType.SELECT)
    @GetMapping("/list")
    public TableDataInfo list(NotificationQueryDTO notificationQueryDTO) {
        startPage();
        List<NotificationVO> notificationList = notificationService.selectNotificationList(notificationQueryDTO);
        return getDataTable(notificationList);
    }

    @Log(title = "查詢抄送給我的通知", businessType = BusinessType.SELECT)
    @GetMapping("/ccToMe")
    public TableDataInfo ccToMe(NotificationQueryDTO notificationQueryDTO) {
        notificationQueryDTO.setUserId(getUserId());
        notificationQueryDTO.setOpenUserId(getOpenUserId());
        List<NotificationVO> notificationList = notificationService.selectCcToMeList(notificationQueryDTO);
        return getDataTable(notificationList);
    }

    @Log(title = "查詢我發送的通知", businessType = BusinessType.SELECT)
    @GetMapping("/mySend")
    public TableDataInfo mySend(NotificationQueryDTO notificationQueryDTO) {
        notificationQueryDTO.setSenderId(getUserId());
        startPage();
        List<NotificationVO> notificationList = notificationService.selectMySendList(notificationQueryDTO);
        return getDataTable(notificationList);
    }

    @Log(title = "查詢通知詳情", businessType = BusinessType.SELECT)
    @GetMapping(value = "/{notificationId}")
    public AjaxResult getInfo(@PathVariable("notificationId") Long notificationId) {
        NotificationVO notificationVO = notificationService.selectNotificationById(notificationId);
        if (notificationVO == null) {
            return AjaxResult.error("通知不存在");
        }

        NotificationDetailVO notificationDetailVO = new NotificationDetailVO();
        notificationDetailVO.setNotification(notificationVO);
        notificationDetailVO.setReceivers(notificationReceiverService.selectByNotificationId(notificationId));
        notificationDetailVO.setCcs(notificationCcService.selectByNotificationId(notificationId));
        notificationDetailVO.setQuestions(notificationQuestionService.selectByNotificationId(notificationId));
        notificationDetailVO.setSendStatistics(notificationSendRecordService.getSendStatisticsVO(notificationId));
        notificationDetailVO.setReadStatistics(notificationUserReadRecordService.getReadStatisticsVO(notificationId));

        return AjaxResult.success(notificationDetailVO);
    }

    @Log(title = "發佈通知", businessType = BusinessType.INSERT)
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult add(@RequestBody NotificationSaveDTO notificationSaveDTO) {
        notificationSaveDTO.setSenderId(getUserId());
        notificationSaveDTO.setSenderName(
                sysUserRoleService.resolveSenderDisplayName(getOpenUserId(), getUsername()));

        try {
            if (notificationService.save(notificationSaveDTO, getUsername())) {
                if ("1".equals(notificationSaveDTO.getStatus())) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            CompletableFuture.runAsync(() -> {
                                try {
                                    notificationPublishHandler.publishAfterSave(notificationSaveDTO.getNotificationId());
                                } catch (Exception e) {
                                    logger.error("異步發送通知失敗: {}", e.getMessage(), e);
                                }
                            });
                        }
                    });
                }
                return AjaxResult.success();
            }
            return AjaxResult.error("發佈通知");
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "提示家長回復", businessType = BusinessType.UPDATE)
    @PostMapping("/remindParents/{notificationId}")
    public AjaxResult remindParents(@PathVariable Long notificationId) {
        try {
            Map<String, Object> result = notificationPublishHandler.remindParentsToReply(notificationId);
            Boolean success = (Boolean) result.get("success");
            if (success != null && success) {
                return AjaxResult.success(result);
            }
            return AjaxResult.error(402, (String) result.get("message"));
        } catch (Exception e) {
            return AjaxResult.error(402, "提示家長回復失敗: " + e.getMessage());
        }
    }

    @Log(title = "重新發送失敗通知", businessType = BusinessType.UPDATE)
    @PostMapping("/resendFailed/{notificationId}")
    public AjaxResult resendFailed(@PathVariable Long notificationId) {
        try {
            Map<String, Object> result = notificationPublishHandler.resendFailedNotifications(notificationId, false);
            Boolean success = (Boolean) result.get("success");
            if (success != null && success) {
                return AjaxResult.success(result);
            }
            return AjaxResult.error(402, (String) result.get("message"));
        } catch (Exception e) {
            return AjaxResult.error(402, "重發失敗通知失敗: " + e.getMessage());
        }
    }

    @Log(title = "撤回通知", businessType = BusinessType.UPDATE)
    @PostMapping("/recall/{notificationId}")
    public AjaxResult recall(@PathVariable Long notificationId) {
        try {
            NotificationVO notificationVO = notificationService.selectNotificationById(notificationId);
            if (notificationVO == null) {
                return AjaxResult.error("通告不存在");
            }

            // 2. 執行資料庫更新
            boolean success = notificationService.recallNotification(notificationId, getUsername());
            if (success) {
                // 3. 異步發送撤回微信通知
                CompletableFuture.runAsync(() -> {
                    try {
                        notificationPublishHandler.sendRecallNotification(notificationId);
                    } catch (Exception e) {
                        logger.error("發送撤回微信通知失敗: {}", e.getMessage(), e);
                    }
                });
                return AjaxResult.success("撤回成功");
            }
            return AjaxResult.error("撤回失敗，通告可能已被撤回或狀態不正確");
        } catch (Exception e) {
            logger.error("撤回通知失敗: {}", e.getMessage(), e);
            return AjaxResult.error("撤回失敗: " + e.getMessage());
        }
    }

    @Log(title = "導出通知回復答案", businessType = BusinessType.EXPORT)
    @GetMapping("/exportAnswers/{notificationId}")
    public void exportAnswers(@PathVariable Long notificationId, HttpServletResponse response) {
        notificationExportService.exportNotificationAnswers(notificationId, response);
    }

    @Log(title = "查詢失敗通知列表", businessType = BusinessType.SELECT)
    @GetMapping("/failedList")
    public TableDataInfo failedList() {
        String userId = getOpenUserId();
        if (!sysUserRoleService.hasAdminUserRole(userId)) {
            return getDataTable(new ArrayList<>());
        }

        startPage();
        List<FailedNotificationVO> failedNotificationList = failedNotificationService.selectFailedNotificationList();
        return getDataTable(failedNotificationList);
    }

    @Log(title = "查詢失敗通知詳情", businessType = BusinessType.SELECT)
    @GetMapping("/failedDetail/{sendRecordId}")
    public AjaxResult failedDetail(@PathVariable Long sendRecordId) {
        String userId = getOpenUserId();
        if (!sysUserRoleService.hasAdminUserRole(userId)) {
            return AjaxResult.error("無權限訪問");
        }

        FailedNotificationDetailVO failedNotificationDetailVO =
                failedNotificationService.selectFailedNotificationDetail(sendRecordId);
        if (failedNotificationDetailVO == null) {
            return AjaxResult.error("未找到相關數據");
        }

        return AjaxResult.success(failedNotificationDetailVO);
    }

    @Log(title = "查詢失敗通知閱讀記錄", businessType = BusinessType.SELECT)
    @GetMapping("/failedReadRecords/{sendRecordId}")
    public TableDataInfo failedReadRecords(@PathVariable Long sendRecordId) {
        String userId = getOpenUserId();
        if (!sysUserRoleService.hasAdminUserRole(userId)) {
            return getDataTable(new ArrayList<>());
        }

        startPage();
        List<UserReadRecordVO> userReadRecordList = failedNotificationService.selectFailedReadRecordsPage(sendRecordId);
        return getDataTable(userReadRecordList);
    }

    @Log(title = "查詢重發失敗記錄", businessType = BusinessType.SELECT)
    @GetMapping("/resendFailRecords/{sendRecordId}")
    public TableDataInfo resendFailRecords(@PathVariable Long sendRecordId) {
        String userId = getOpenUserId();
        if (!sysUserRoleService.hasAdminUserRole(userId)) {
            return getDataTable(new ArrayList<>());
        }

        startPage();
        List<ResendFailRecordVO> resendFailRecordList = failedNotificationService.selectResendFailRecordsPage(sendRecordId);
        return getDataTable(resendFailRecordList);
    }
}
