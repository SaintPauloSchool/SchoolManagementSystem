package com.sp.web.controller;

import com.sp.common.annotation.Anonymous;
import com.sp.common.core.controller.BaseController;
import com.sp.common.core.domain.AjaxResult;
import com.sp.system.entity.SysSchoolDepartment;
import com.sp.system.entity.SysSchoolDepartmentMember;
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
     * 根据 ID 查询部门详情
     */
    @Anonymous
    @GetMapping("/{id}")
    public AjaxResult getDepartment(@PathVariable Long id) {
        SysSchoolDepartment department = sysSchoolDepartmentService.getDepartmentById(id);
        return AjaxResult.success(department);
    }

    /**
     * 新增部门
     */
    @Anonymous
    @PostMapping
    public AjaxResult addDepartment(@RequestBody SysSchoolDepartment department) {
        return toAjax(sysSchoolDepartmentService.addDepartment(department));
    }

    /**
     * 修改部门
     */
    @Anonymous
    @PutMapping
    public AjaxResult updateDepartment(@RequestBody SysSchoolDepartment department) {
        return toAjax(sysSchoolDepartmentService.updateDepartment(department));
    }

    /**
     * 删除部门
     */
    @Anonymous
    @DeleteMapping("/{id}")
    public AjaxResult deleteDepartment(@PathVariable Long id) {
        return toAjax(sysSchoolDepartmentService.deleteDepartmentById(id));
    }

    /**
     * 新增部门成员
     */
    @Anonymous
    @PostMapping("/member")
    public AjaxResult addDepartmentMember(@RequestBody SysSchoolDepartmentMember member) {
        return toAjax(sysSchoolDepartmentService.addDepartmentMember(member));
    }

    /**
     * 修改部门成员
     */
    @Anonymous
    @PutMapping("/member")
    public AjaxResult updateDepartmentMember(@RequestBody SysSchoolDepartmentMember member) {
        return toAjax(sysSchoolDepartmentService.updateDepartmentMember(member));
    }

    /**
     * 删除部门成员
     */
    @Anonymous
    @DeleteMapping("/member/{id}")
    public AjaxResult deleteDepartmentMember(@PathVariable Long id) {
        return toAjax(sysSchoolDepartmentService.deleteDepartmentMemberById(id));
    }
}
