package com.sms.system.entity.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 每日學生手冊通知發送範圍設置（type=1 班級部門，可多選）
 */
public class DailyNoticeClassSettingDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 每日通知發送範圍內的班級部門 ID 列表（type=1） */
    private List<Long> classDepartmentIds;

    public List<Long> getClassDepartmentIds() {
        return classDepartmentIds;
    }

    public void setClassDepartmentIds(List<Long> classDepartmentIds) {
        this.classDepartmentIds = classDepartmentIds;
    }
}
