package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 單筆同步結果寫入請求（同步成功時更新家校通訊錄企微姓名）
 */
public class SysStudentMatchSyncRecordDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long matchId;
    private String userId;
    private String studentUserId;
    private String syncTargetName;
    /** 1 成功，2 失敗 */
    private String syncStatus;
    private String operName;

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(String studentUserId) {
        this.studentUserId = studentUserId;
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

    public String getOperName() {
        return operName;
    }

    public void setOperName(String operName) {
        this.operName = operName;
    }
}
