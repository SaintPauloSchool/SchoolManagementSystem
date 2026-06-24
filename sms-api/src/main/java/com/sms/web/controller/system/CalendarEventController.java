package com.sms.web.controller.system;

import com.sms.common.annotation.Log;
import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.enums.BusinessType;
import com.sms.system.entity.dto.CalendarEventDeleteDTO;
import com.sms.system.entity.dto.CalendarEventQueryDTO;
import com.sms.system.entity.dto.CalendarEventSaveDTO;
import com.sms.system.entity.vo.CalendarEventVO;
import com.sms.system.service.ICalendarEventService;
import com.sms.system.service.ISysAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/system/calendarEvent")
public class CalendarEventController extends BaseController {

    @Autowired
    private ICalendarEventService calendarEventService;

    @Autowired
    private ISysAdminService sysAdminService;

    private boolean isNotAdmin() {
        return sysAdminService.isNotAdmin(getOpenUserId());
    }

    @Log(title = "查詢行事曆事件列表", businessType = BusinessType.SELECT)
    @GetMapping("/list")
    public TableDataInfo list(CalendarEventQueryDTO calendarEventQueryDTO) {
        if (isNotAdmin()) {
            return getDataTable(new ArrayList<>());
        }
        startPage();
        List<CalendarEventVO> calendarEventList = calendarEventService.selectCalendarEventList(calendarEventQueryDTO);
        return getDataTable(calendarEventList);
    }

    @Log(title = "查詢行事曆事件詳情", businessType = BusinessType.SELECT)
    @GetMapping(value = "/{eventId}")
    public AjaxResult getInfo(@PathVariable("eventId") Long eventId) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        return AjaxResult.success(calendarEventService.selectCalendarEventByEventId(eventId));
    }

    @Log(title = "新增行事曆事件", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CalendarEventSaveDTO calendarEventSaveDTO) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        return toAjax(calendarEventService.insertCalendarEvent(calendarEventSaveDTO, getUsername()));
    }

    @Log(title = "行事曆事件-批量新增", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public AjaxResult addBatch(@RequestBody List<CalendarEventSaveDTO> calendarEventSaveDTOList) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        return toAjax(calendarEventService.insertCalendarEventBatch(calendarEventSaveDTOList, getUsername()));
    }

    @Log(title = "修改行事曆事件", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CalendarEventSaveDTO calendarEventSaveDTO) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        return toAjax(calendarEventService.updateCalendarEvent(calendarEventSaveDTO, getUsername()));
    }

    @Log(title = "刪除行事曆事件", businessType = BusinessType.DELETE)
    @DeleteMapping("/{eventIds}")
    public AjaxResult remove(@PathVariable Long[] eventIds) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        CalendarEventDeleteDTO calendarEventDeleteDTO = new CalendarEventDeleteDTO();
        calendarEventDeleteDTO.setEventIds(eventIds);
        return toAjax(calendarEventService.deleteCalendarEventByEventIds(calendarEventDeleteDTO));
    }

    @Log(title = "導入行事曆事件", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file) throws Exception {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        String message = calendarEventService.importCalendarEvent(file, getUsername());
        return AjaxResult.success(message);
    }

    @Log(title = "下載行事曆導入模板", businessType = BusinessType.EXPORT)
    @GetMapping("/importTemplate")
    public void downloadTemplate(HttpServletResponse response) throws Exception {
        if (isNotAdmin()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        calendarEventService.downloadImportTemplate(response);
    }
}
