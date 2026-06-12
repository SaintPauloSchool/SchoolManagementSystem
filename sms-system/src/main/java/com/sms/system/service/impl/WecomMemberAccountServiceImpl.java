package com.sms.system.service.impl;

import com.sms.system.entity.WecomMemberAccount;
import com.sms.system.mapper.WecomMemberAccountMapper;
import com.sms.system.service.IWecomMemberAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * 企微成員登入帳號 業務層實作
 */
@Service
public class WecomMemberAccountServiceImpl implements IWecomMemberAccountService {

    private static final Logger log = LoggerFactory.getLogger(WecomMemberAccountServiceImpl.class);

    /** 系統 Pepper 靜態密鑰，由 yml 注入，增強加密強度 */
    @Value("${wechat.work.memberAccountPepper:fKf8bX_CG0azo_odDEVP_T15DIL_xNmWtM}")
    private String pepper;

    @Autowired
    private WecomMemberAccountMapper wecomMemberAccountMapper;

    @Override
    public WecomMemberAccount selectByUserId(String userid) {
        return wecomMemberAccountMapper.selectByUserId(userid);
    }

    @Override
    public WecomMemberAccount selectByUsername(String username) {
        return wecomMemberAccountMapper.selectByUsername(username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveAccount(String userid, String username, String password, String status) {
        // 1. 檢查帳號是否存在
        WecomMemberAccount existingAccount = wecomMemberAccountMapper.selectByUserId(userid);

        // 2. 檢查登入帳號 username 是否被其他人佔用
        WecomMemberAccount usernameCheck = wecomMemberAccountMapper.selectByUsername(username);
        if (usernameCheck != null && !usernameCheck.getUserid().equals(userid)) {
            throw new RuntimeException("帳號名稱已被佔用");
        }

        if (existingAccount == null) {
            // 新增帳號
            if (password == null || password.trim().isEmpty()) {
                throw new RuntimeException("密碼不能為空");
            }
            WecomMemberAccount account = new WecomMemberAccount();
            account.setUserid(userid);
            account.setUsername(username);
            account.setStatus(status);

            // 生成隨機鹽值并加密密碼
            String salt = generateSalt();
            account.setSalt(salt);
            account.setPassword(hashPassword(password, salt));

            return wecomMemberAccountMapper.insert(account) > 0;
        } else {
            // 更新帳號
            existingAccount.setUsername(username);
            existingAccount.setStatus(status);

            // 如果輸入了新密碼，則重新生成隨機鹽並加密
            if (password != null && !password.trim().isEmpty()) {
                String salt = generateSalt();
                existingAccount.setSalt(salt);
                existingAccount.setPassword(hashPassword(password, salt));
            }

            return wecomMemberAccountMapper.update(existingAccount) > 0;
        }
    }

    /**
     * 生成 16 位安全隨機鹽值
     */
    private String generateSalt() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 加密密碼：BCrypt(SHA256(rawPassword + salt + pepper))
     */
    private String hashPassword(String rawPassword, String salt) {
        String data = rawPassword + salt + pepper;
        String sha256Hex = getSha256Hex(data);
        BCryptPasswordEncoder bcryptEncoder = new BCryptPasswordEncoder();
        return bcryptEncoder.encode(sha256Hex);
    }

    /**
     * SHA-256 雜湊
     */
    private String getSha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            log.error("SHA-256 雜湊計算失敗：", ex);
            throw new RuntimeException("安全雜湊失敗", ex);
        }
    }
}
