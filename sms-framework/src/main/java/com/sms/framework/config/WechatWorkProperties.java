package com.sms.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "wechat.work")
public class WechatWorkProperties {

    private String corpId;
    private Integer agentId;
    private String secret;
    private String redirectUri;
    private Callback callback = new Callback();

    public String getCorpId() {
        return corpId;
    }

    public void setCorpId(String corpId) {
        this.corpId = corpId;
    }

    public Integer getAgentId() {
        return agentId;
    }

    public void setAgentId(Integer agentId) {
        this.agentId = agentId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public Callback getCallback() {
        return callback;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public static class Callback {
        private String token;
        private String encodingAesKey;
        private String corpId;
        private String permanentCode;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getEncodingAesKey() {
            return encodingAesKey;
        }

        public void setEncodingAesKey(String encodingAesKey) {
            this.encodingAesKey = encodingAesKey;
        }

        public String getCorpId() {
            return corpId;
        }

        public void setCorpId(String corpId) {
            this.corpId = corpId;
        }

        public String getPermanentCode() {
            return permanentCode;
        }

        public void setPermanentCode(String permanentCode) {
            this.permanentCode = permanentCode;
        }
    }
}