package com.sms.web.controller.base;

import com.sms.common.core.controller.BaseController;
import com.sms.system.service.ISysUserRoleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 需 sys_user_role 權限的 Controller 基類
 */
public abstract class SysUserRoleBaseController extends BaseController {

    @Autowired
    private ISysUserRoleService sysUserRoleService;

    /**
     * 是否無權限訪問學生相關 / 系統管理功能（僅 type 0/1 可訪問，type 2 其他不可見）
     */
    protected boolean isNotUserRole() {
        return !sysUserRoleService.hasAdminUserRole(getOpenUserId());
    }

    protected boolean hasUserRole() {
        return sysUserRoleService.hasAdminUserRole(getOpenUserId());
    }

    protected boolean isNotSuperUserRole() {
        return sysUserRoleService.isNotSuperUserRole(getOpenUserId());
    }

    protected boolean hasSuperUserRole() {
        return !isNotSuperUserRole();
    }
}
