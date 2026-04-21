package com.sms.framework.wechat;

import com.alibaba.fastjson.JSONObject;
import com.sms.framework.config.WechatWorkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class WechatWorkHttpClient {

    private static final Logger log = LoggerFactory.getLogger(WechatWorkHttpClient.class);

    private static final String ACCESS_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid={corpId}&corpsecret={corpSecret}";
    private static final String SEND_MESSAGE_URL = "https://qyapi.weixin.qq.com/cgi-bin/externalcontact/message/send?access_token={accessToken}";

    @Autowired
    private WechatWorkProperties wechatWorkProperties;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ConcurrentHashMap<String, TokenCache> tokenCache = new ConcurrentHashMap<>();

    public String getAccessToken() {
        String corpId = wechatWorkProperties.getCorpId();
        TokenCache cache = tokenCache.get(corpId);

        if (cache != null && !cache.isExpired()) {
            return cache.getAccessToken();
        }

        try {
            String url = ACCESS_TOKEN_URL.replace("{corpId}", corpId)
                .replace("{corpSecret}", wechatWorkProperties.getSecret());

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JSONObject jsonObject = JSONObject.parseObject(response.getBody());

            if (jsonObject.getInteger("errcode") == 0) {
                String accessToken = jsonObject.getString("access_token");
                Integer expiresIn = jsonObject.getInteger("expires_in");
                tokenCache.put(corpId, new TokenCache(accessToken, System.currentTimeMillis() + (expiresIn - 300L) * 1000L));
                log.info("wechat work access token refreshed");
                return accessToken;
            }

            log.error("failed to get wechat work access token: {}", jsonObject.getString("errmsg"));
            throw new RuntimeException("failed to get access token: " + jsonObject.getString("errmsg"));
        } catch (Exception e) {
            log.error("failed to get wechat work access token", e);
            throw new RuntimeException("failed to get access token: " + e.getMessage(), e);
        }
    }

    public JSONObject sendSchoolNotification(JSONObject messageData) {
        try {
            String accessToken = getAccessToken();
            String url = SEND_MESSAGE_URL.replace("{accessToken}", accessToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(messageData.toJSONString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JSONObject result = JSONObject.parseObject(response.getBody());

            if (result.getInteger("errcode") == 0) {
                log.info("wechat work notification sent successfully");
            } else {
                log.error("wechat work notification failed: {} - {}", result.getInteger("errcode"), result.getString("errmsg"));
            }

            return result;
        } catch (Exception e) {
            log.error("failed to send wechat work notification", e);
            throw new RuntimeException("failed to send notification: " + e.getMessage(), e);
        }
    }

    private static class TokenCache {
        private final String accessToken;
        private final long expireTime;

        private TokenCache(String accessToken, long expireTime) {
            this.accessToken = accessToken;
            this.expireTime = expireTime;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expireTime;
        }
    }
}