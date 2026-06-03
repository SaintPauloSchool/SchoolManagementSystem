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
     * 微信 OAuth 授權 URL
     */
    @Value("${wechat.work.api.oauthAuthorizeUrl:https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_base&agentid=%s&state=%s#wechat_redirect}")
    private String oauthAuthorizeUrl;

    /**
     * 獲取 Access Token 的 API 接口地址
     */
    @Value("${wechat.work.api.accessTokenUrl:https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid={corpId}&corpsecret={corpSecret}}")
    private String accessTokenUrl;
    
    /**
     * 發送家校通知消息的 API 接口地址
     */
    @Value("${wechat.work.api.sendMessageUrl:https://qyapi.weixin.qq.com/cgi-bin/externalcontact/message/send?access_token={accessToken}}")
    private String sendMessageUrl;

    /**
     * 發送應用消息的 API 接口地址
     */
    @Value("${wechat.work.api.sendAppMessageUrl:https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token={accessToken}}")
    private String sendAppMessageUrl;

    /**
     * 獲取企業部門列表 API
     */
    @Value("${wechat.work.api.departmentListUrl:https://qyapi.weixin.qq.com/cgi-bin/department/list?access_token={accessToken}}")
    private String departmentListUrl;

    /**
     * 獲取企業部門成員 API
     */
    @Value("${wechat.work.api.departmentMemberListUrl:https://qyapi.weixin.qq.com/cgi-bin/user/simplelist?access_token={accessToken}&department_id={departmentId}}")
    private String departmentMemberListUrl;

    /**
     * 獲取家校通訊錄部門 API
     */
    @Value("${wechat.work.api.schoolDepartmentListUrl:https://qyapi.weixin.qq.com/cgi-bin/school/department/list?access_token={accessToken}}")
    private String schoolDepartmentListUrl;

    /**
     * 獲取家校通訊錄家長列表 API
     */
    @Value("${wechat.work.api.schoolParentListUrl:https://qyapi.weixin.qq.com/cgi-bin/school/user/list_parent?access_token={accessToken}&department_id={departmentId}}")
    private String schoolParentListUrl;

    /**
     * 企業微信 CropId
     */
    @Value("${wechat.work.corpId:ww04fad852e91fd490}")
    private String corpId;

    /**
     * 企業微信 AgentId（應用ID）
     */
    @Value("${wechat.work.agentId:1000033}")
    private Integer agentId;

    /**
     * 企業微信 Secret
     */
    @Value("${wechat.work.secret:I31-B5clKayPf4vNl2bibhL1ia8x4cwIc884xK888Fc}")
    private String secret;

    /**
     * 微信 OAuth 回調的重定向 URI
     */
    @Value("${wechat.work.oauthRedirectUri:https://mo-stu-sys.org-assistant.com/sp-api/wechat/oauth/callback}")
    private String oauthRedirectUri;

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

        // 雙重檢查鎖 (Double-Checked Locking)，避免併發時多個執行緒同時去微信請求 Token
        synchronized (this) {
            cache = tokenCache.get(corpId);
            if (cache != null && !cache.isExpired()) {
                return cache.getAccessToken();
            }

            try {
                // 構造請求 URL
                String url = accessTokenUrl.replace("{corpId}", corpId)
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
            String url = sendMessageUrl.replace("{accessToken}", accessToken);
            
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
     * 發送應用消息（內部應用消息推送）
     * 用於向企業內部成員發送消息
     *
     * @param messageData 消息 JSON 數據內容
     * @return 微信接口調用結果
     */
    public JSONObject sendAppMessage(JSONObject messageData) {
        try {
            validateMessageConfig();
            String accessToken = getAccessToken();
            String url = sendAppMessageUrl.replace("{accessToken}", accessToken);
            
            // 發送 POST 請求到微信接口
            String response = HttpUtils.sendPost(url, messageData.toJSONString(), MediaType.APPLICATION_JSON_VALUE);
            JSONObject result = JSONObject.parseObject(response);

            if (result == null) {
                throw new RuntimeException("發送應用消息失敗：回應為空");
            }

            // 判斷發送結果
            if (result.getInteger("errcode") == 0) {
                log.info("微信應用消息已成功發送");
            } else {
                log.error("微信應用消息發送失敗: {} - {}", result.getInteger("errcode"), result.getString("errmsg"));
            }

            return result;
        } catch (Exception e) {
            log.error("微信應用消息發送失敗", e);
            throw new RuntimeException("發送應用消息失敗: " + e.getMessage(), e);
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

    /**
     * 獲取企業部門列表
     * @return 微信接口調用結果 (JSONObject)
     */
    public JSONObject getDepartmentList() {
        try {
            String accessToken = getAccessToken();
            String url = departmentListUrl.replace("{accessToken}", accessToken);
            String response = HttpUtils.sendGet(url);
            return JSONObject.parseObject(response);
        } catch (Exception e) {
            log.error("獲取企業部門列表失敗", e);
            throw new RuntimeException("獲取企業部門列表失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 獲取企業部門成員列表
     * @param departmentId 部門ID
     * @return 微信接口調用結果 (JSONObject)
     */
    public JSONObject getDepartmentMembers(Long departmentId) {
        try {
            String accessToken = getAccessToken();
            String url = departmentMemberListUrl.replace("{accessToken}", accessToken)
                                                .replace("{departmentId}", String.valueOf(departmentId));
            String response = HttpUtils.sendGet(url);
            return JSONObject.parseObject(response);
        } catch (Exception e) {
            log.error("獲取企業部門成員失敗", e);
            throw new RuntimeException("獲取企業部門成員失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 獲取家校通訊錄部門列表
     * @return 微信接口調用結果 (JSONObject)
     */
    public JSONObject getSchoolDepartmentList() {
        try {
            String accessToken = getAccessToken();
            String url = schoolDepartmentListUrl.replace("{accessToken}", accessToken);
            String response = HttpUtils.sendGet(url);
            return JSONObject.parseObject(response);
        } catch (Exception e) {
            log.error("獲取家校通訊錄部門列表失敗", e);
            throw new RuntimeException("獲取家校通訊錄部門列表失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 獲取家校通訊錄家長列表
     * @param departmentId 班級部門ID
     * @return 微信接口調用結果 (JSONObject)
     */
    public JSONObject getSchoolParentList(Long departmentId) {
        try {
            String accessToken = getAccessToken();
            String url = schoolParentListUrl.replace("{accessToken}", accessToken)
                                            .replace("{departmentId}", String.valueOf(departmentId));
            String response = HttpUtils.sendGet(url);
            return JSONObject.parseObject(response);
        } catch (Exception e) {
            log.error("獲取家校通訊錄家長列表失敗", e);
            throw new RuntimeException("獲取家校通訊錄家長列表失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 構建 WeChat OAuth 授權 URL
     *
     * @param noticeUrl 原 URL
     * @param state     授權攜帶的 state 參數
     * @return 構建後的 OAuth 授權 URL
     */
    public String buildOauthUrl(String noticeUrl, String state) {
        if (noticeUrl == null || noticeUrl.contains("open.weixin.qq.com") || corpId == null || corpId.trim().isEmpty()) {
            return noticeUrl;
        }
        try {
            String encodedRedirectUri = java.net.URLEncoder.encode(oauthRedirectUri, "UTF-8");
            return String.format(oauthAuthorizeUrl, corpId, encodedRedirectUri, agentId, state);
        } catch (Exception e) {
            log.error("構建 WeChat OAuth URL 失敗", e);
            return noticeUrl;
        }
    }
}
