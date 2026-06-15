package com.sms.system.mapper;

import com.sms.system.entity.WecomMemberAccount;
import org.apache.ibatis.annotations.Param;

/**
 * 企微成員登入帳號 Mapper 接口
 */
public interface WecomMemberAccountMapper {

    /**
     * 根據成員 UserID 查詢帳號
     *
     * @param userid 成員 UserID
     * @return 帳號資訊，若不存在則返回 null
     */
    WecomMemberAccount selectByUserId(@Param("userid") String userid);

    /**
     * 根據登入帳號查詢帳號
     *
     * @param username 登入帳號
     * @return 帳號資訊，若不存在則返回 null
     */
    WecomMemberAccount selectByUsername(@Param("username") String username);

    /**
     * 新增帳號
     *
     * @param account 帳號資訊
     * @return 影響行數
     */
    int insert(WecomMemberAccount account);

    /**
     * 更新帳號
     *
     * @param account 帳號資訊
     * @return 影響行數
     */
    int update(WecomMemberAccount account);
}
