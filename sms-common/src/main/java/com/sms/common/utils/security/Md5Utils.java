package com.sms.common.utils.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Md5加密方法
 *
 */
public class Md5Utils
{
    private static final Logger log = LoggerFactory.getLogger(Md5Utils.class);

    private static byte[] md5(String s)
    {
        MessageDigest algorithm;
        try
        {
            algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(s.getBytes(StandardCharsets.UTF_8));
            return algorithm.digest();
        }
        catch (Exception e)
        {
            log.error("MD5 Error...", e);
        }
        return null;
    }

    private static final String toHex(byte hash[])
    {
        if (hash == null)
        {
            return null;
        }
        StringBuffer buf = new StringBuffer(hash.length * 2);
        int i;

        for (i = 0; i < hash.length; i++)
        {
            if ((hash[i] & 0xff) < 0x10)
            {
                buf.append("0");
            }
            buf.append(Long.toString(hash[i] & 0xff, 16));
        }
        return buf.toString();
    }

    public static String hash(String s)
    {
        try
        {
            return new String(Objects.requireNonNull(toHex(md5(s))).getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            log.error("not supported charset...{}", e);
            return s;
        }
    }

    /**
     * 加密敏感 ID（如學生用戶 ID）
     * 將原始 ID 與鹽值組合後進行 MD5 加密
     *
     * @param originalId 原始 ID（如 student_id）
     * @param salt 鹽值
     * @return 加密後的字符串（32位 MD5 哈希值）
     */
    public static String encryptSensitiveId(String originalId, String salt)
    {
        if (originalId == null || originalId.trim().isEmpty())
        {
            return "";
        }

        try
        {
            // 組合原始數據：originalId + 鹽值
            String originalData = originalId + salt;
            
            // MD5 加密
            return hash(originalData);
        }
        catch (Exception e)
        {
            log.error("加密敏感 ID 失敗: {}", originalId, e);
            // 如果加密失敗，返回原始的 MD5（不加鹽）
            return hash(originalId);
        }
    }

}
