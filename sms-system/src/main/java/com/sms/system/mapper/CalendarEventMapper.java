package com.sms.system.mapper;

import com.sms.system.entity.CalendarEvent;
import com.sms.system.entity.vo.CalendarEventVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 行事曆 Mapper 接口
 */
public interface CalendarEventMapper {
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
     * 修改行事曆事件
     */
    int updateCalendarEvent(CalendarEvent calendarEvent);

    /**
     * 刪除行事曆事件
     */
    int deleteCalendarEventByEventId(Long eventId);

    /**
     * 批量刪除行事曆事件
     */
    int deleteCalendarEventByEventIds(@Param("eventIds") Long[] eventIds);
    
    /**
     * 批量插入行事曆事件
     */
    int insertCalendarEventBatch(List<CalendarEvent> list);
}
