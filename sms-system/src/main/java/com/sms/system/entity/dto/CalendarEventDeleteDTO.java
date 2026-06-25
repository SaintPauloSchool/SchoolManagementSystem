package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 行事曆批量刪除請求
 */
public class CalendarEventDeleteDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long[] eventIds;

    public Long[] getEventIds() {
        return eventIds;
    }

    public void setEventIds(Long[] eventIds) {
        this.eventIds = eventIds;
    }
}
