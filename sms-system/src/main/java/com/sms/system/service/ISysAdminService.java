package com.sms.system.service;

import com.sms.system.entity.SysAdmin;

/**
 * 系统管理员 Service 接口
 */
public interface ISysAdminService {
    /**
     * 根据用户ID查询管理员信息
     *
     * @param userId 用户ID
     * @return 管理员信息
     */
    SysAdmin selectByUserId(String userId);

    /**
     * 验证用户是否「非」管理员
     *
     * @param userId 用户ID
     * @return true-不是管理员 false-是管理员
     */
    boolean isNotAdmin(String userId);
}
