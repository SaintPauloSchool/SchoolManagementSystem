package com.sms.system.service.impl;

import com.sms.system.entity.SysAdmin;
import com.sms.system.mapper.SysAdminMapper;
import com.sms.system.service.ISysAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 系統管理員 Service 業務層處理
 */
@Service
public class SysAdminServiceImpl implements ISysAdminService {

    @Autowired
    private SysAdminMapper sysAdminMapper;

    /**
     * 驗證用戶是否「非」管理員
     *
     * @param userId 用戶ID
     * @return true-不是管理員 false-是管理員
     */
    @Override
    public boolean isNotAdmin(String userId) {
        SysAdmin admin = sysAdminMapper.selectByUserId(userId);
        return admin == null || !"0".equals(admin.getStatus());
    }
}
