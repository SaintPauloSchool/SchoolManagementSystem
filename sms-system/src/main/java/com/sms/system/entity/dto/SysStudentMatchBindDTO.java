package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 手動綁定學生匹配請求
 */
public class SysStudentMatchBindDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long matchId;
    private String studentProfileNum;
    private String studentUserIdWecom;

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

    public String getStudentUserIdWecom() {
        return studentUserIdWecom;
    }

    public void setStudentUserIdWecom(String studentUserIdWecom) {
        this.studentUserIdWecom = studentUserIdWecom;
    }
}
