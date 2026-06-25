package com.sms.system.entity.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 批量同步學生姓名至企微請求
 */
public class SysStudentMatchSyncDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Long> matchIds;

    /** 操作人（由控制層注入，非前端傳入） */
    private String operName;

    public List<Long> getMatchIds() {
        return matchIds;
    }

    public void setMatchIds(List<Long> matchIds) {
        this.matchIds = matchIds;
    }

    public String getOperName() {
        return operName;
    }

    public void setOperName(String operName) {
        this.operName = operName;
    }
}
