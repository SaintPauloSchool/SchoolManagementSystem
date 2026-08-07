package com.sms.web.controller.system;

import com.sms.common.annotation.Log;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.enums.BusinessType;
import com.sms.system.entity.dto.SysAdminBatchSaveDTO;
import com.sms.system.entity.dto.SysAdminQueryDTO;
import com.sms.system.entity.dto.SysAdminUpdateDTO;
import com.sms.system.entity.vo.SysAdminBatchInsertResultVO;
import com.sms.system.entity.vo.SysAdminCurrentUserVO;
import com.sms.system.entity.vo.SysAdminVO;
import com.sms.system.service.ISysAdminService;
import com.sms.web.controller.base.AdminBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 系統管理員 Controller
 */
@RestController
@RequestMapping("/system/admin")
public class SysAdminController extends AdminBaseController {

    @Autowired
    private ISysAdminService sysAdminService;

    /**
     * 查詢當前登入用戶管理員權限
     */
    @Log(title = "查詢當前用戶管理員權限", businessType = BusinessType.SELECT)
    @GetMapping("/checkCurrentUser")
    public AjaxResult checkCurrentUser() {
        SysAdminCurrentUserVO vo = sysAdminService.selectCurrentUserInfo(getOpenUserId());
        return AjaxResult.success(vo);
    }

    /**
     * 管理員列表（僅超級管理員）
     */
    @Log(title = "查詢系統管理員列表", businessType = BusinessType.SELECT)
    @GetMapping("/list")
    public TableDataInfo list(SysAdminQueryDTO queryDTO) {
        if (isNotSuperAdmin()) {
            return getDataTable(new ArrayList<>());
        }
        startPage();
        List<SysAdminVO> list = sysAdminService.selectList(queryDTO);
        return getDataTable(list);
    }

    /**
     * 管理員詳情（僅超級管理員）
     */
    @Log(title = "查詢系統管理員詳情", businessType = BusinessType.SELECT)
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        if (isNotSuperAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        SysAdminVO vo = sysAdminService.selectById(id);
        return AjaxResult.success(vo);
    }

    /**
     * 從 WeCom 老師通訊錄批量新增管理員（僅超級管理員）
     */
    @Log(title = "批量新增系統管理員", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public AjaxResult batchAdd(@RequestBody SysAdminBatchSaveDTO saveDTO) {
        if (isNotSuperAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        SysAdminBatchInsertResultVO vo = sysAdminService.batchInsert(saveDTO);
        return AjaxResult.success(vo);
    }

    /**
     * 修改管理員（僅超級管理員）
     */
    @Log(title = "修改系統管理員", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysAdminUpdateDTO updateDTO) {
        if (isNotSuperAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        return toAjax(sysAdminService.updateAdmin(updateDTO, getOpenUserId()));
    }

    /**
     * 刪除管理員（僅超級管理員）
     */
    @Log(title = "刪除系統管理員", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        if (isNotSuperAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        return toAjax(sysAdminService.deleteByIds(ids, getOpenUserId()));
    }
}
