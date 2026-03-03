package com.sp.web.controller.system.notification;

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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Arrays;

/**
 * 通知Controller
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
     * 查询通知列表
     */
    @PreAuthorize("@ss.hasPermi('system:notification:list')")
    @GetMapping("/list")
    public TableDataInfo list(Notification notification) {
        startPage();
        List<Notification> list = notificationService.selectNotificationList(notification);
        return getDataTable(list);
    }

    /**
     * 查询抄送给我的通知列表
     */
    @PreAuthorize("@ss.hasPermi('system:notification:ccToList')")
    @GetMapping("/ccToMe")
    public TableDataInfo ccToMe() {
        // 获取当前登录用户信息
        Long userId = getUserId();
        String userType = getUserType(); // 需要在BaseController中实现
        
        startPage();
        List<Notification> list = notificationService.selectCcToMeList(userId, userType);
        return getDataTable(list);
    }

    /**
     * 查询我发送的通知列表
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
     * 获取通知详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:notification:query')")
    @GetMapping(value = "/{notificationId}")
    public AjaxResult getInfo(@PathVariable("notificationId") Long notificationId) {
        Notification notification = notificationService.selectNotificationById(notificationId);
        if (notification == null) {
            return AjaxResult.error("通知不存在");
        }
        
        // 获取接收对象信息
        List<NotificationReceiver> receivers = notificationReceiverService.selectByNotificationId(notificationId);
        // 获取抄送对象信息
        List<NotificationCc> ccs = notificationCcService.selectByNotificationId(notificationId);
        // 获取问题信息
        List<NotificationQuestion> questions = notificationQuestionService.selectByNotificationId(notificationId);
        
        AjaxResult result = AjaxResult.success(notification);
        result.put("receivers", receivers);
        result.put("ccs", ccs);
        result.put("questions", questions);
        
        return result;
    }

    /**
     * 发布通知
     */
    @PreAuthorize("@ss.hasPermi('system:notification:add')")
    @Log(title = "发布通知", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Notification notification) {
        // 设置发送人信息
        notification.setSenderId(getUserId());
        notification.setSenderName(getUsername()); // 需要在BaseController中实现
        
        int result = notificationService.insertNotification(notification);
        return result > 0 ? AjaxResult.success() : AjaxResult.error("发布失败");
    }

    /**
     * 修改通知
     */
    @PreAuthorize("@ss.hasPermi('system:notification:edit')")
    @Log(title = "修改通知", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Notification notification) {
        int result = notificationService.updateNotification(notification);
        return result > 0 ? AjaxResult.success() : AjaxResult.error("修改失败");
    }

    /**
     * 删除通知
     */
    @PreAuthorize("@ss.hasPermi('system:notification:remove')")
    @Log(title = "删除通知", businessType = BusinessType.DELETE)
    @DeleteMapping("/{notificationIds}")
    public AjaxResult remove(@PathVariable Long[] notificationIds) {
        int result = notificationService.deleteNotificationByIds(Arrays.asList(notificationIds));
        return result > 0 ? AjaxResult.success() : AjaxResult.error("删除失败");
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
        
        notification.setStatus("2"); // 设置为已撤回状态
        int result = notificationService.updateNotification(notification);
        return result > 0 ? AjaxResult.success() : AjaxResult.error("撤回失败");
    }
}