package com.sms.web.controller.system;

import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.system.entity.WecomMemberAccount;
import com.sms.system.service.IWecomMemberAccountService;
import com.sms.system.service.ISysAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 企微成員帳號管理 控制層
 */
@RestController
@RequestMapping("/system/wecom/account")
public class WecomMemberAccountController extends BaseController {

    @Autowired
    private IWecomMemberAccountService wecomMemberAccountService;

    @Autowired
    private ISysAdminService sysAdminService;

    /**
     * 校驗是否非管理員
     */
    private boolean isNotAdmin() {
        return sysAdminService.isNotAdmin(getOpenUserId());
    }

    /**
     * 查詢指定成員的帳號基本資訊
     *
     * @param userid 成員 UserID
     * @return 帳號詳情（排除密碼欄位）
     */
    @GetMapping("/info")
    public AjaxResult getAccountInfo(@RequestParam("userid") String userid) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問，僅管理員有權進行此操作");
        }
        
        WecomMemberAccount account = wecomMemberAccountService.selectByUserId(userid);
        if (account == null) {
            return AjaxResult.success("該成員尚未設置登入帳號", null);
        }

        // 隱藏敏感欄位，僅返回基本帳號資訊與狀態
        Map<String, Object> data = new HashMap<>();
        data.put("userid", account.getUserid());
        data.put("username", account.getUsername());
        data.put("status", account.getStatus());
        data.put("createTime", account.getCreateTime());
        data.put("updateTime", account.getUpdateTime());
        
        return AjaxResult.success("查詢成功", data);
    }

    /**
     * 新增或修改帳號資訊
     *
     * @param params 請求參數，包含 userid, username, password, status
     * @return 操作結果
     */
    @PostMapping("/save")
    public AjaxResult saveAccount(@RequestBody Map<String, String> params) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問，僅管理員有權進行此操作");
        }

        String userid = params.get("userid");
        String username = params.get("username");
        String password = params.get("password");
        String status = params.get("status");

        if (userid == null || userid.trim().isEmpty()) {
            return AjaxResult.error("成員 UserID 不能為空");
        }
        if (username == null || username.trim().isEmpty()) {
            return AjaxResult.error("帳號名稱不能為空");
        }
        if (status == null || status.trim().isEmpty()) {
            status = "0"; // 預設為正常狀態
        }

        try {
            boolean success = wecomMemberAccountService.saveAccount(userid, username.trim(), password, status);
            return success ? AjaxResult.success("儲存帳號資訊成功") : AjaxResult.error("儲存帳號資訊失敗");
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
