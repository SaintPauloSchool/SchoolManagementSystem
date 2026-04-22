package com.sms.framework.wechat;

import com.alibaba.fastjson.JSONObject;
import com.sms.common.utils.http.HttpUtils;
import com.sms.system.entity.TokenCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 企業微信 HTTP 客戶端
 * 用於封裝與企業微信服務器交互的接口調用，例如獲取 Access Token、發送消息等。
 */
@Component
public class WechatWorkHttpClient {

    private static final Logger log = LoggerFactory.getLogger(WechatWorkHttpClient.class);

    /**
     * 獲取 Access Token 的 API 接口地址
     */
    private static final String ACCESS_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid={corpId}&corpsecret={corpSecret}";
    
    /**
     * 發送家校通知消息的 API 接口地址
     */
    private static final String SEND_MESSAGE_URL = "https://qyapi.weixin.qq.com/cgi-bin/externalcontact/message/send?access_token={accessToken}";

    /**
     * 企業微信 CropId
     */
    @Value("${wechat.work.corpId:}")
    private String corpId;

    /**
     * 企業微信 AgentId（應用ID）
     */
    @Value("${wechat.work.agentId:#{null}}")
    private Integer agentId;

    /**
     * 企業微信 Secret
     */
    @Value("${wechat.work.secret:}")
    private String secret;

    /**
     * 緩存 Token，避免頻繁調用微信接口請求 Token，導致觸發頻率限制
     * Key 為 CorpId，Value 為對應的 Token 緩存對象
     */
    private final ConcurrentHashMap<String, TokenCache> tokenCache = new ConcurrentHashMap<>();

    /**
     * 獲取有效的 Access Token
     * 如果緩存中的 Token 仍有效，則直接返回；否則調用微信接口重新獲取並更新緩存。
     *
     * @return 企業微信 Access Token
     */
    public String getAccessToken() {
        validateBaseConfig();
        TokenCache cache = tokenCache.get(corpId);

        // 如果緩存存在且沒有過期，則直接使用緩存的 accessToken
        if (cache != null && !cache.isExpired()) {
            return cache.getAccessToken();
        }

        try {
            // 構造請求 URL
            String url = ACCESS_TOKEN_URL.replace("{corpId}", corpId)
                .replace("{corpSecret}", secret);

            log.info("請求微信獲取access token");
            String response = HttpUtils.sendGet(url);
            JSONObject jsonObject = JSONObject.parseObject(response);

            if (jsonObject == null) {
                throw new RuntimeException("取得access token失敗：回應為空");
            }

            // errcode 為 0 表示請求成功
            if (jsonObject.getInteger("errcode") == 0) {
                String accessToken = jsonObject.getString("access_token");
                Integer expiresIn = jsonObject.getInteger("expires_in");
                
                // token 有效期為 expiresIn 秒，我們減去 300 秒作為緩衝，避免在邊界點發生 Token 失效
                long expireTimeMs = System.currentTimeMillis() + (expiresIn - 300L) * 1000L;
                tokenCache.put(corpId, new TokenCache(accessToken, expireTimeMs));
                
                log.info("微信access token已刷新");
                return accessToken;
            }

            log.error("取得微信access token失敗: {}", jsonObject.getString("errmsg"));
            throw new RuntimeException("取得微信access token失敗: " + jsonObject.getString("errmsg"));
        } catch (Exception e) {
            log.error("取得微信access token失敗", e);
            throw new RuntimeException("取得微信access token失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 發送家校通知消息（基於外部聯繫人能力）
     *
     * @param messageData 消息 JSON 數據內容
     * @return 微信接口調用結果
     */
    public JSONObject sendSchoolNotification(JSONObject messageData) {
        try {
            validateMessageConfig();
            String accessToken = getAccessToken();
            String url = SEND_MESSAGE_URL.replace("{accessToken}", accessToken);
            
            // 發送 POST 請求到微信接口
            String response = HttpUtils.sendPost(url, messageData.toJSONString(), MediaType.APPLICATION_JSON_VALUE);
            JSONObject result = JSONObject.parseObject(response);

            if (result == null) {
                throw new RuntimeException("發送通知失敗：回應為空");
            }

            // 判斷發送結果
            if (result.getInteger("errcode") == 0) {
                log.info("微信通知已成功發送");
            } else {
                log.error("微信通知發送失敗: {} - {}", result.getInteger("errcode"), result.getString("errmsg"));
            }

            return result;
        } catch (Exception e) {
            log.error("微信通知發送失敗", e);
            throw new RuntimeException("發送通知失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 驗證基礎配置（企業 CorpId 和 Secret 必須存在）
     */
    private void validateBaseConfig() {
        if (!StringUtils.hasText(corpId)) {
            throw new IllegalStateException("缺失corpId配置項");
        }
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("缺失secret配置項");
        }
    }

    /**
     * 驗證消息發送相關配置（發送消息額外需要 AgentId）
     */
    private void validateMessageConfig() {
        validateBaseConfig();
        if (agentId == null) {
            throw new IllegalStateException("缺失agentId配置項");
        }
    }
}
