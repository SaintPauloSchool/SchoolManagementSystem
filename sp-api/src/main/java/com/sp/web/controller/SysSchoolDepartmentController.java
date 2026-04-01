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
     * 获取学校部门树形结构
     */
    @Anonymous
    @GetMapping("/tree")
    public AjaxResult tree() {
        List<SysSchoolDepartment> tree = sysSchoolDepartmentService.getSysSchoolDepartmentTree();
        return AjaxResult.success(tree);
    }

    /**
     * 批量查询多个部门的成员列表
     */
    @Anonymous
    @PostMapping("/members")
    public AjaxResult getMembersByDepartments(@RequestBody List<Long> departmentIds) {
        List<SysSchoolDepartmentMember> members = sysSchoolDepartmentMemberService.getMembersByDepartmentIds(departmentIds);
        return AjaxResult.success(members);
    }

    /**
     * 根据 ID 删除部门成员
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
     * 根据 ID 删除部门
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
}
