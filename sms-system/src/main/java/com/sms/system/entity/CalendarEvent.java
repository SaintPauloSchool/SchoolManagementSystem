package com.sms.system.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sms.common.core.domain.BaseEntity;

import java.time.LocalDate;

/**
 * 行事曆表 calendar_event
 */
public class CalendarEvent extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 事件ID */
    private Long eventId;

    /** 事件日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate eventDate;

    /** 事件標題 */
    private String title;

    /** 對象類型（0: 全校, 1: 幼稚園, 2: 小學, 3: 中學） */
    private Integer targetType;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getTargetType() {
        return targetType;
    }

    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }
}
