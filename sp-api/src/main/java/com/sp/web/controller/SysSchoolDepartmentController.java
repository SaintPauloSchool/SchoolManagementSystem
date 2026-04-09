package com.sp.web.controller;

import com.sp.common.annotation.Anonymous;
import com.sp.common.core.controller.BaseController;
import com.sp.common.core.domain.AjaxResult;
import com.sp.system.entity.SysSchoolDepartment;
import com.sp.system.entity.SysSchoolDepartmentMember;
import com.sp.system.service.ISysSchoolDepartmentMemberService;
import com.sp.system.service.ISysSchoolDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统学校部门信息管理
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
    @Anonymous
    @GetMapping("/tree")
    public AjaxResult tree(@RequestParam(required = false, defaultValue = "1") Integer type) {
        List<SysSchoolDepartment> tree = sysSchoolDepartmentService.getSysSchoolDepartmentTree(type);
        return AjaxResult.success(tree);
    }

    /**
     * 獲取學校部門樹形結構（包含人員節點）
     */
    @Anonymous
    @GetMapping("/treeWithMembers")
    public AjaxResult treeWithMembers(@RequestParam(required = false, defaultValue = "1") Integer type) {
        List<SysSchoolDepartment> tree = sysSchoolDepartmentService.getSysSchoolDepartmentTreeWithMembers(type);
        return AjaxResult.success(tree);
    }

    /**
     * 批量查詢多個部門的成員列表
     */
    @Anonymous
    @PostMapping("/members")
    public AjaxResult getMembersByDepartments(@RequestBody List<Long> departmentIds) {
        List<SysSchoolDepartmentMember> members = sysSchoolDepartmentMemberService.getMembersByDepartmentIds(departmentIds);
        return AjaxResult.success(members);
    }

    /**
     * 根據 ID 刪除部門成員
     */
    @Anonymous
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
    @Anonymous
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
    @Anonymous
    @PostMapping("/members/batch")
    public AjaxResult batchAddMembers(@RequestBody List<SysSchoolDepartmentMember> members,
                                      @RequestParam(required = false, defaultValue = "1") Integer type) {
        if (members == null || members.isEmpty()) {
            return AjaxResult.error("成員列表不能為空");
        }
        
        // 為每個成員設置 type
        for (SysSchoolDepartmentMember member : members) {
            if (member.getType() == null) {
                member.setType(type);
            }
        }
        
        int result = sysSchoolDepartmentMemberService.batchAddMembers(members);
        if (result > 0) {
            return AjaxResult.success("添加 " + result + " 名成員成功");
        } else {
            return AjaxResult.error("添加成員失敗");
        }
    }

    /**
     * 新增部門
     */
    @Anonymous
    @PostMapping
    public AjaxResult addDepartment(@RequestBody SysSchoolDepartment department,
                                    @RequestParam(required = false, defaultValue = "1") Integer type) {
        // 設置默認 type
        if (department.getType() == null) {
            department.setType(type);
        }
        int result = sysSchoolDepartmentService.insertSysSchoolDepartment(department);
        if (result > 0) {
            return AjaxResult.success("新增成功");
        } else {
            return AjaxResult.error("新增失敗");
        }
    }

    /**
     * 修改部門
     */
    @Anonymous
    @PutMapping
    public AjaxResult editDepartment(@RequestBody SysSchoolDepartment department) {
        int result = sysSchoolDepartmentService.updateSysSchoolDepartment(department);
        if (result > 0) {
            return AjaxResult.success("修改成功");
        } else {
            return AjaxResult.error("修改失敗");
        }
    }
}
