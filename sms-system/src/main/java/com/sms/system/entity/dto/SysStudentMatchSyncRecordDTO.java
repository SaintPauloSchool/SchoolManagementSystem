package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 單筆同步結果寫入請求
 */
public class SysStudentMatchSyncRecordDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long matchId;
    private String studentUserIdWecom;
    private String syncTargetName;
    private String syncStatus;
    private String errorMsg;
    private String operName;

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public String getStudentUserIdWecom() {
        return studentUserIdWecom;
    }

    public void setStudentUserIdWecom(String studentUserIdWecom) {
        this.studentUserIdWecom = studentUserIdWecom;
    }

    public String getSyncTargetName() {
        return syncTargetName;
    }

    public void setSyncTargetName(String syncTargetName) {
        this.syncTargetName = syncTargetName;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getOperName() {
        return operName;
    }

    public void setOperName(String operName) {
        this.operName = operName;
    }
}
