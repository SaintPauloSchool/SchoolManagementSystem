package com.sms.web.controller.system;

import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.system.service.ISysAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系統管理員 Controller
 */
@RestController
@RequestMapping("/system/admin")
public class SysAdminController extends BaseController {

    @Autowired
    private ISysAdminService sysAdminService;

    /**
     * 查詢當前登入用戶是否為管理員
     * 前端用於控制「系統管理」側邊欄的顯示/隱藏
     */
    @GetMapping("/checkCurrentUser")
    public AjaxResult checkCurrentUser() {
        boolean isAdmin = !sysAdminService.isNotAdmin(getOpenUserId());
        return AjaxResult.success(isAdmin);
    }
}
