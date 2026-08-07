package com.sms.web.controller.base;

import com.sms.common.core.controller.BaseController;
import com.sms.system.service.ISysAdminService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 需管理員權限的 Controller 基類
 * <p>BaseController 位於 sms-common，無法依賴 ISysAdminService，故在此擴展。</p>
 */
public abstract class AdminBaseController extends BaseController {

    @Autowired
    private ISysAdminService sysAdminService;

    protected boolean isNotAdmin() {
        return sysAdminService.isNotAdmin(getOpenUserId());
    }

    protected boolean isAdmin() {
        return !isNotAdmin();
    }

    /** 非超級管理員（含未登入、普通管理員、停用） */
    protected boolean isNotSuperAdmin() {
        return sysAdminService.isNotSuperAdmin(getOpenUserId());
    }

    protected boolean isSuperAdmin() {
        return !isNotSuperAdmin();
    }
}
