package com.sms.system.entity.vo;

import com.sms.common.core.domain.BaseEntity;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class CalendarEventVO extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate eventDate;

    private String title;

    private Integer targetType;

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
