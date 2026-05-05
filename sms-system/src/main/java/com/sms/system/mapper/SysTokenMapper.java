package com.sms.system.mapper;

import com.sms.system.entity.SysToken;
import org.apache.ibatis.annotations.Param;

/**
 * sys_token 表 Mapper 接口
 */
public interface SysTokenMapper {

    /**
     * 根據 Token 值查詢記錄
     *
     * @param token Token 字符串
     * @return SysToken，若不存在則返回 null
     */
    SysToken selectByToken(@Param("token") String token);
}
