package com.sms.web.controller.system;

import com.sms.common.annotation.Log;
import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.enums.BusinessType;
import com.sms.system.entity.CalendarEvent;
import com.sms.system.service.ICalendarEventService;
import com.sms.system.service.ISysAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/system/calendarEvent")
public class CalendarEventController extends BaseController {

    @Autowired
    private ICalendarEventService calendarEventService;

    @Autowired
    private ISysAdminService sysAdminService;

    /**
     * 校驗是否為管理員
     */
    private boolean checkAdmin() {
        return sysAdminService.isAdmin(getOpenUserId());
    }

    /**
     * 查詢行事曆列表
     */
    @GetMapping("/list")
    public TableDataInfo list(CalendarEvent calendarEvent) {
        if (!checkAdmin()) {
            return getDataTable(new ArrayList<>());
        }
        startPage();
        List<CalendarEvent> list = calendarEventService.selectCalendarEventList(calendarEvent);
        return getDataTable(list);
    }

    /**
     * 獲取詳細資訊
     */
    @GetMapping(value = "/{eventId}")
    public AjaxResult getInfo(@PathVariable("eventId") Long eventId) {
        if (!checkAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        return AjaxResult.success(calendarEventService.selectCalendarEventByEventId(eventId));
    }

    /**
     * 新增行事曆事件
     */
    @Log(title = "行事曆事件", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CalendarEvent calendarEvent) {
        if (!checkAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        calendarEvent.setCreateBy(getUsername());
        return toAjax(calendarEventService.insertCalendarEvent(calendarEvent));
    }

    /**
     * 批量新增行事曆事件（日期範圍展開）
     */
    @Log(title = "行事曆事件-批量新增", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public AjaxResult addBatch(@RequestBody List<CalendarEvent> calendarEvents) {
        if (!checkAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        String username = getUsername();
        calendarEvents.forEach(e -> e.setCreateBy(username));
        return toAjax(calendarEventService.insertCalendarEventBatch(calendarEvents));
    }

    /**
     * 修改行事曆事件
     */
    @Log(title = "行事曆事件", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CalendarEvent calendarEvent) {
        if (!checkAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        calendarEvent.setUpdateBy(getUsername());
        return toAjax(calendarEventService.updateCalendarEvent(calendarEvent));
    }

    /**
     * 刪除行事曆事件
     */
    @Log(title = "行事曆事件", businessType = BusinessType.DELETE)
    @DeleteMapping("/{eventIds}")
    public AjaxResult remove(@PathVariable Long[] eventIds) {
        if (!checkAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        return toAjax(calendarEventService.deleteCalendarEventByEventIds(eventIds));
    }

    /**
     * 導入行事曆事件
     */
    @Log(title = "行事曆事件", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file) throws Exception {
        if (!checkAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        String message = calendarEventService.importCalendarEvent(file, getUsername());
        return AjaxResult.success(message);
    }
}
