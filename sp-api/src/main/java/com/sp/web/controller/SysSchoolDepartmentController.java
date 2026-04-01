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
}
