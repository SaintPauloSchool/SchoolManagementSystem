package com.sms.system.service;

import com.sms.system.entity.CalendarEvent;
import com.sms.system.entity.vo.CalendarEventVO;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * 行事曆 Service 接口
 */
public interface ICalendarEventService {
    /**
     * 查詢行事曆
     */
    CalendarEvent selectCalendarEventByEventId(Long eventId);

    /**
     * 查詢行事曆列表
     */
    List<CalendarEvent> selectCalendarEventList(CalendarEventVO eventVO);

    /**
     * 新增行事曆事件
     */
    int insertCalendarEvent(CalendarEvent calendarEvent);

    /**
     * 批量新增行事曆事件
     */
    int insertCalendarEventBatch(List<CalendarEvent> calendarEvents);

    /**
     * 修改行事曆事件
     */
    int updateCalendarEvent(CalendarEvent calendarEvent);

    /**
     * 批量刪除行事曆事件
     */
    int deleteCalendarEventByEventIds(Long[] eventIds);

    /**
     * 導入行事曆事件
     */
    String importCalendarEvent(MultipartFile file, String operName) throws Exception;
}
