package com.sms.system.service;

import com.sms.system.entity.WecomMemberAccount;

/**
 * 企微成員登入帳號 業務層介面
 */
public interface IWecomMemberAccountService {

    /**
     * 根據成員 UserID 查詢帳號資訊
     *
     * @param userid 成員 UserID
     * @return 帳號資訊
     */
    WecomMemberAccount selectByUserId(String userid);

    /**
     * 根據登入帳號查詢帳號資訊
     *
     * @param username 登入帳號
     * @return 帳號資訊
     */
    WecomMemberAccount selectByUsername(String username);

    /**
     * 新增或更新帳號資訊 (密碼會經由隨機鹽 Salt + SHA-256 + 系統 Pepper + BCrypt 多重加密)
     *
     * @param userid   成員 UserID
     * @param username 登入帳號
     * @param password 明文密碼（如果更新時此值為空，則代表不修改密碼）
     * @param status   帳號狀態（0正常 1停用）
     * @return 是否操作成功
     */
    boolean saveAccount(String userid, String username, String password, String status);
}
