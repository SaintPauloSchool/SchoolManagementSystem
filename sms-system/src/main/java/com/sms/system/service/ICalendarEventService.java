package com.sms.system.service;

import com.sms.system.entity.dto.CalendarEventDeleteDTO;
import com.sms.system.entity.dto.CalendarEventQueryDTO;
import com.sms.system.entity.dto.CalendarEventSaveDTO;
import com.sms.system.entity.vo.CalendarEventVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 行事曆 Service 接口
 */
public interface ICalendarEventService {
    /**
     * 查詢行事曆
     */
    CalendarEventVO selectCalendarEventByEventId(Long eventId);

    /**
     * 查詢行事曆列表
     */
    List<CalendarEventVO> selectCalendarEventList(CalendarEventQueryDTO calendarEventQueryDTO);

    /**
     * 新增行事曆事件
     */
    int insertCalendarEvent(CalendarEventSaveDTO calendarEventSaveDTO, String createBy);

    /**
     * 批量新增行事曆事件
     */
    int insertCalendarEventBatch(List<CalendarEventSaveDTO> calendarEventSaveDTOList, String createBy);

    /**
     * 修改行事曆事件
     */
    int updateCalendarEvent(CalendarEventSaveDTO calendarEventSaveDTO, String updateBy);

    /**
     * 批量刪除行事曆事件
     */
    int deleteCalendarEventByEventIds(CalendarEventDeleteDTO calendarEventDeleteDTO);

    /**
     * 導入行事曆事件
     */
    String importCalendarEvent(MultipartFile file, String operName) throws Exception;

    /**
     * 下載行事曆導入模版
     */
    void downloadImportTemplate(HttpServletResponse response) throws Exception;
}
