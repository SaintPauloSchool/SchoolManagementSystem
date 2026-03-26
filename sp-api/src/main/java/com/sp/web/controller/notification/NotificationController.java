package com.sp.web.controller.notification;

import com.sp.common.annotation.Log;
import com.sp.common.core.controller.BaseController;
import com.sp.common.core.domain.AjaxResult;
import com.sp.common.core.page.TableDataInfo;
import com.sp.common.enums.BusinessType;
import com.sp.system.entity.notification.Notification;
import com.sp.system.entity.notification.NotificationReceiver;
import com.sp.system.entity.notification.NotificationCc;
import com.sp.system.entity.notification.NotificationQuestion;
import com.sp.system.service.notification.INotificationService;
import com.sp.system.service.notification.INotificationReceiverService;
import com.sp.system.service.notification.INotificationCcService;
import com.sp.system.service.notification.INotificationQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Arrays;
import java.util.Date;

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

    /**
     * 查詢通知列表
     */
    @PreAuthorize("@ss.hasPermi('system:notification:list')")
    @GetMapping("/list")
    public TableDataInfo list(Notification notification) {
        startPage();
        List<Notification> list = notificationService.selectNotificationList(notification);
        return getDataTable(list);
    }

    /**
     * 查詢抄送給我的通知列表
     */
    @PreAuthorize("@ss.hasPermi('system:notification:ccToList')")
    @GetMapping("/ccToMe")
    public TableDataInfo ccToMe() {
        // 獲取當前登錄用戶信息
        Long userId = getUserId();
        String userType = getUserType();
        
        startPage();
        List<Notification> list = notificationService.selectCcToMeList(userId, userType);
        return getDataTable(list);
    }

    /**
     * 查詢我發送的通知列表
     */
    @PreAuthorize("@ss.hasPermi('system:notification:mySend')")
    @GetMapping("/mySend")
    public TableDataInfo mySend() {
        Long senderId = getUserId();
        startPage();
        List<Notification> list = notificationService.selectMySendList(senderId);
        return getDataTable(list);
    }

    /**
     * 獲取通知詳細信息
     */
    @PreAuthorize("@ss.hasPermi('system:notification:query')")
    @GetMapping(value = "/{notificationId}")
    public AjaxResult getInfo(@PathVariable("notificationId") Long notificationId) {
        Notification notification = notificationService.selectNotificationById(notificationId);
        if (notification == null) {
            return AjaxResult.error("通知不存在");
        }
        
        // 獲取接收對象信息
        List<NotificationReceiver> receivers = notificationReceiverService.selectByNotificationId(notificationId);
        // 獲取抄送對象信息
        List<NotificationCc> ccs = notificationCcService.selectByNotificationId(notificationId);
        // 獲取問題信息
        List<NotificationQuestion> questions = notificationQuestionService.selectByNotificationId(notificationId);
        
        AjaxResult result = AjaxResult.success(notification);
        result.put("receivers", receivers);
        result.put("ccs", ccs);
        result.put("questions", questions);
        
        return result;
    }

    /**
     * 發布通知
     */
    @PreAuthorize("@ss.hasPermi('system:notification:add')")
    @Log(title = "发布通知", businessType = BusinessType.INSERT)
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult add(@RequestBody Notification notification) {
        // 設置發送人信息
        notification.setSenderId(getUserId());
        notification.setSenderName(getUsername());
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
            
            return AjaxResult.success();
        } else {
            return AjaxResult.error("發布失敗");
        }
    }

    /**
     * 修改通知
     */
    @PreAuthorize("@ss.hasPermi('system:notification:edit')")
    @Log(title = "修改通知", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Notification notification) {
        int result = notificationService.updateNotification(notification);
        return result > 0 ? AjaxResult.success() : AjaxResult.error("修改失敗");
    }

    /**
     * 刪除通知
     */
    @PreAuthorize("@ss.hasPermi('system:notification:remove')")
    @Log(title = "删除通知", businessType = BusinessType.DELETE)
    @DeleteMapping("/{notificationIds}")
    public AjaxResult remove(@PathVariable Long[] notificationIds) {
        int result = notificationService.deleteNotificationByIds(Arrays.asList(notificationIds));
        return result > 0 ? AjaxResult.success() : AjaxResult.error("刪除失敗");
    }

    /**
     * 撤回通知
     */
    @PreAuthorize("@ss.hasPermi('system:notification:withdraw')")
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
}