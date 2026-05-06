package com.sms.system.entity;

import java.util.Date;

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
    private Date expireTime;

    /** 創建時間 */
    private Date createTime;

    /** 更新時間 */
    private Date updateTime;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Integer getUserType() { return userType; }
    public void setUserType(Integer userType) { this.userType = userType; }

    public Date getExpireTime() { return expireTime; }
    public void setExpireTime(Date expireTime) { this.expireTime = expireTime; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    /**
     * 判斷 Token 是否已過期
     */
    public boolean isExpired() {
        return expireTime != null && new Date().after(expireTime);
    }
}
