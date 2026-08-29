package com.sms.web.controller.system;

import com.sms.common.annotation.Log;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.enums.BusinessType;
import com.sms.system.entity.dto.SysUserRoleBatchSaveDTO;
import com.sms.system.entity.dto.SysUserRoleQueryDTO;
import com.sms.system.entity.dto.SysUserRoleUpdateDTO;
import com.sms.system.entity.vo.SysUserRoleBatchInsertResultVO;
import com.sms.system.entity.vo.SysUserRoleCurrentUserVO;
import com.sms.system.entity.vo.SysUserRoleVO;
import com.sms.system.service.ISysUserRoleService;
import com.sms.web.controller.base.SysUserRoleBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 系統用戶角色 Controller
 */
@RestController
@RequestMapping("/system/userRole")
public class SysUserRoleController extends SysUserRoleBaseController {

    @Autowired
    private ISysUserRoleService sysUserRoleService;

    @Log(title = "查詢當前用戶角色", businessType = BusinessType.SELECT)
    @GetMapping("/checkCurrentUser")
    public AjaxResult checkCurrentUser() {
        SysUserRoleCurrentUserVO vo = sysUserRoleService.selectCurrentUserInfo(getOpenUserId(), getUsername());
        return AjaxResult.success(vo);
    }

    @Log(title = "查詢用戶角色列表", businessType = BusinessType.SELECT)
    @GetMapping("/list")
    public TableDataInfo list(SysUserRoleQueryDTO queryDTO) {
        if (isNotSuperUserRole()) {
            return getDataTable(new ArrayList<>());
        }
        startPage();
        List<SysUserRoleVO> list = sysUserRoleService.selectList(queryDTO);
        return getDataTable(list);
    }

    @Log(title = "查詢用戶角色詳情", businessType = BusinessType.SELECT)
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        if (isNotSuperUserRole()) {
            return AjaxResult.error("無權限訪問");
        }
        SysUserRoleVO vo = sysUserRoleService.selectById(id);
        return AjaxResult.success(vo);
    }

    @Log(title = "批量新增用戶角色", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public AjaxResult batchAdd(@RequestBody SysUserRoleBatchSaveDTO saveDTO) {
        if (isNotSuperUserRole()) {
            return AjaxResult.error("無權限訪問");
        }
        SysUserRoleBatchInsertResultVO vo = sysUserRoleService.batchInsert(saveDTO);
        return AjaxResult.success(vo);
    }

    @Log(title = "修改用戶角色", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysUserRoleUpdateDTO updateDTO) {
        if (isNotSuperUserRole()) {
            return AjaxResult.error("無權限訪問");
        }
        return toAjax(sysUserRoleService.updateUserRole(updateDTO, getOpenUserId()));
    }

    @Log(title = "刪除用戶角色", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        if (isNotSuperUserRole()) {
            return AjaxResult.error("無權限訪問");
        }
        return toAjax(sysUserRoleService.deleteByIds(ids, getOpenUserId()));
    }
}
