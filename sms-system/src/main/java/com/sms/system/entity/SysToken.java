package com.sms.system.entity;

import java.time.LocalDateTime;

/**
 * sys_token 表實體類
 */
public class SysToken {

    /** 主鍵 ID */
    private Long id;

    /** 用戶 ID */
    private String userId;

    /** Token 值 */
    private String token;

    /** 用戶類型 (0: 學生, 1: 家長, 2: 員工) */
    private Integer userType;

    /** 過期時間 */
    private LocalDateTime expireTime;

    /** 創建時間 */
    private LocalDateTime createTime;

    /** 更新時間 */
    private LocalDateTime updateTime;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Integer getUserType() { return userType; }
    public void setUserType(Integer userType) { this.userType = userType; }

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    /**
     * 判斷 Token 是否已過期
     */
    public boolean isExpired() {
        return expireTime != null && LocalDateTime.now().isAfter(expireTime);
    }
}
