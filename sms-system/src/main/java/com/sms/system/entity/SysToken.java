package com.sms.system.entity;

import java.util.Date;

/**
 * sys_token 表實體類
 */
public class SysToken {

    /** 主鍵 ID */
    private Long id;

    /** 用戶 ID（對應管理後台的 sys_user.user_id） */
    private Long userId;

    /** 家長用戶 ID（企業微信 userId，可為空） */
    private String parentUserId;

    /** Token 值 */
    private String token;

    /** 過期時間 */
    private Date expireTime;

    /** 創建時間 */
    private Date createTime;

    /** 更新時間 */
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getParentUserId() { return parentUserId; }
    public void setParentUserId(String parentUserId) { this.parentUserId = parentUserId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

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
