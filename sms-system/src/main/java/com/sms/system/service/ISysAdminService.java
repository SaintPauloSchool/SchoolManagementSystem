package com.sms.system.service;

import com.sms.system.entity.SysAdmin;

/**
 * 系統管理員 Service 接口
 */
public interface ISysAdminService {

    /**
     * 驗證用戶是否「非」管理員
     *
     * @param userId 用戶ID
     * @return true-不是管理員 false-是管理員
     */
    boolean isNotAdmin(String userId);
}
