package com.sms.system.entity;

/**
 * 企業微信 Access Token 緩存實體類
 */
public class TokenCache {
    /**
     * 訪問憑證
     */
    private final String accessToken;
    
    /**
     * 過期時間（時間戳，毫秒）
     */
    private final long expireTime;

    public TokenCache(String accessToken, long expireTime) {
        this.accessToken = accessToken;
        this.expireTime = expireTime;
    }

    public String getAccessToken() {
        return accessToken;
    }

    /**
     * 判斷 Token 是否已過期
     * @return true 如果已過期，否則 false
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= expireTime;
    }
}
