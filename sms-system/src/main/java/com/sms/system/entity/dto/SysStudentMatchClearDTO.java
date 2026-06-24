package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 清除學生匹配關係請求
 */
public class SysStudentMatchClearDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long matchId;
    private String studentProfileNum;

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public String getStudentProfileNum() {
        return studentProfileNum;
    }

    public void setStudentProfileNum(String studentProfileNum) {
        this.studentProfileNum = studentProfileNum;
    }
}
