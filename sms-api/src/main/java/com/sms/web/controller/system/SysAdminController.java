package com.sms.web.controller.system;

import com.sms.common.annotation.Log;
import com.sms.web.controller.base.AdminBaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.enums.BusinessType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系統管理員 Controller
 */
@RestController
@RequestMapping("/system/admin")
public class SysAdminController extends AdminBaseController {

    /**
     * 查詢當前登入用戶是否為管理員
     * 前端用於控制「系統管理」側邊欄的顯示/隱藏
     */
    @Log(title = "查詢當前用戶管理員權限", businessType = BusinessType.SELECT)
    @GetMapping("/checkCurrentUser")
    public AjaxResult checkCurrentUser() {
        return AjaxResult.success(isAdmin());
    }
}
