package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 手動綁定學生匹配請求
 */
public class SysStudentMatchBindDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long matchId;
    private String studentId;
    private String userId;

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
