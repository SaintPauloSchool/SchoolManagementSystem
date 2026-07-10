package com.sms.web.controller;

import com.sms.common.annotation.Log;
import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.enums.BusinessType;
import com.sms.system.entity.dto.SysSchoolDepartmentMemberBatchSaveDTO;
import com.sms.system.entity.dto.SysSchoolDepartmentMemberQueryDTO;
import com.sms.system.entity.dto.SysSchoolDepartmentQueryDTO;
import com.sms.system.entity.dto.SysSchoolDepartmentSaveDTO;
import com.sms.system.entity.vo.SysSchoolDepartmentMemberVO;
import com.sms.system.entity.vo.SysSchoolDepartmentVO;
import com.sms.system.service.ISysSchoolDepartmentMemberService;
import com.sms.system.service.ISysSchoolDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系統學校部門資訊管理
 *
 */
@RestController
@RequestMapping("/system/schoolDepartment")
public class SysSchoolDepartmentController extends BaseController {

    @Autowired
    private ISysSchoolDepartmentService sysSchoolDepartmentService;

    @Autowired
    private ISysSchoolDepartmentMemberService sysSchoolDepartmentMemberService;

    /**
     * 獲取學校部門樹形結構
     */
    @Log(title = "查詢學校部門樹", businessType = BusinessType.SELECT)
    @GetMapping("/tree")
    public AjaxResult tree(@RequestParam(required = false, defaultValue = "1") Integer type) {
        SysSchoolDepartmentQueryDTO sysSchoolDepartmentQueryDTO = new SysSchoolDepartmentQueryDTO();
        sysSchoolDepartmentQueryDTO.setType(type);
        List<SysSchoolDepartmentVO> sysSchoolDepartmentTree = sysSchoolDepartmentService.getSysSchoolDepartmentTree(sysSchoolDepartmentQueryDTO.getType());
        return AjaxResult.success(sysSchoolDepartmentTree);
    }

    /**
     * 獲取學校部門樹形結構（包含人員節點）
     */
    @Log(title = "查詢學校部門樹（含成員）", businessType = BusinessType.SELECT)
    @GetMapping("/treeWithMembers")
    public AjaxResult treeWithMembers(@RequestParam(required = false, defaultValue = "1") Integer type) {
        SysSchoolDepartmentQueryDTO sysSchoolDepartmentQueryDTO = new SysSchoolDepartmentQueryDTO();
        sysSchoolDepartmentQueryDTO.setType(type);
        List<SysSchoolDepartmentVO> sysSchoolDepartmentTree = sysSchoolDepartmentService.getSysSchoolDepartmentTreeWithMembers(sysSchoolDepartmentQueryDTO.getType());
        return AjaxResult.success(sysSchoolDepartmentTree);
    }

    /**
     * 批量查詢多個部門的成員列表
     */
    @Log(title = "批量查詢部門成員", businessType = BusinessType.SELECT)
    @PostMapping("/members")
    public AjaxResult getMembersByDepartments(@RequestBody SysSchoolDepartmentMemberQueryDTO sysSchoolDepartmentMemberQueryDTO) {
        List<SysSchoolDepartmentMemberVO> sysSchoolDepartmentMemberList = sysSchoolDepartmentMemberService.getMembersByDepartmentIds(sysSchoolDepartmentMemberQueryDTO);
        return AjaxResult.success(sysSchoolDepartmentMemberList);
    }

    /**
     * 根據 ID 刪除部門成員
     */
    @Log(title = "刪除學校部門成員", businessType = BusinessType.DELETE)
    @DeleteMapping("/member/{id}")
    public AjaxResult deleteMember(@PathVariable Long id) {
        int result = sysSchoolDepartmentMemberService.deleteMemberById(id);
        if (result > 0) {
            return AjaxResult.success("刪除成功");
        } else {
            return AjaxResult.error("刪除失敗，成員不存在或已被刪除");
        }
    }

    /**
     * 根據 ID 刪除部門
     */
    @Log(title = "刪除學校部門", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult deleteDepartment(@PathVariable Long id) {
        int result = sysSchoolDepartmentService.deleteSysSchoolDepartmentById(id);
        if (result > 0) {
            return AjaxResult.success("刪除成功");
        } else {
            return AjaxResult.error("刪除失敗，部門不存在或已被刪除");
        }
    }

    /**
     * 批量添加部門成員
     */
    @Log(title = "批量添加學校部門成員", businessType = BusinessType.INSERT)
    @PostMapping("/members/batch")
    public AjaxResult batchAddMembers(@RequestBody SysSchoolDepartmentMemberBatchSaveDTO sysSchoolDepartmentMemberBatchSaveDTO) {
        if (sysSchoolDepartmentMemberBatchSaveDTO == null || sysSchoolDepartmentMemberBatchSaveDTO.getMembers() == null || sysSchoolDepartmentMemberBatchSaveDTO.getMembers().isEmpty()) {
            return AjaxResult.error("成員列表不能為空");
        }
        if (sysSchoolDepartmentMemberBatchSaveDTO.getType() == null) {
            sysSchoolDepartmentMemberBatchSaveDTO.setType(1);
        }

        int result = sysSchoolDepartmentMemberService.batchAddMembers(sysSchoolDepartmentMemberBatchSaveDTO);
        if (result > 0) {
            return AjaxResult.success("添加 " + result + " 名成員成功");
        } else {
            return AjaxResult.error("添加成員失敗");
        }
    }

    /**
     * 新增部門
     */
    @Log(title = "新增學校部門", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult addDepartment(@RequestBody SysSchoolDepartmentSaveDTO sysSchoolDepartmentSaveDTO) {
        if (sysSchoolDepartmentSaveDTO.getType() == null) {
            sysSchoolDepartmentSaveDTO.setType(1);
        }
        int result = sysSchoolDepartmentService.insertSysSchoolDepartment(sysSchoolDepartmentSaveDTO);
        if (result > 0) {
            return AjaxResult.success("新增成功");
        } else {
            return AjaxResult.error("新增失敗");
        }
    }

    /**
     * 修改部門
     */
    @Log(title = "修改學校部門", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult editDepartment(@RequestBody SysSchoolDepartmentSaveDTO sysSchoolDepartmentSaveDTO) {
        int result = sysSchoolDepartmentService.updateSysSchoolDepartment(sysSchoolDepartmentSaveDTO);
        if (result > 0) {
            return AjaxResult.success("修改成功");
        } else {
            return AjaxResult.error("修改失敗");
        }
    }
}
