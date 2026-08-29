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

    protected boolean isNotUserRole() {
        return sysUserRoleService.isNotUserRole(getOpenUserId());
    }

    protected boolean hasUserRole() {
        return !isNotUserRole();
    }

    protected boolean isNotSuperUserRole() {
        return sysUserRoleService.isNotSuperUserRole(getOpenUserId());
    }

    protected boolean hasSuperUserRole() {
        return !isNotSuperUserRole();
    }
}
