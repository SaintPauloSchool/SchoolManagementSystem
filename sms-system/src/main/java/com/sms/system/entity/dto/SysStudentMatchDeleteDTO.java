package com.sms.system.entity.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 批量刪除學生匹配記錄請求
 */
public class SysStudentMatchDeleteDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Long> matchIds;

    public List<Long> getMatchIds() {
        return matchIds;
    }

    public void setMatchIds(List<Long> matchIds) {
        this.matchIds = matchIds;
    }
}
