package com.sms.system.service.impl;

import com.sms.system.entity.SysAdmin;
import com.sms.system.mapper.SysAdminMapper;
import com.sms.system.service.ISysAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 系统管理员 Service 业务层处理
 */
@Service
public class SysAdminServiceImpl implements ISysAdminService {

    @Autowired
    private SysAdminMapper sysAdminMapper;

    /**
     * 验证用户是否「非」管理员
     *
     * @param userId 用户ID
     * @return true-不是管理员 false-是管理员
     */
    @Override
    public boolean isNotAdmin(String userId) {
        SysAdmin admin = sysAdminMapper.selectByUserId(userId);
        return admin == null || !"0".equals(admin.getStatus());
    }
}
