package com.sp.web.controller;

import com.sp.common.annotation.Anonymous;
import com.sp.common.core.controller.BaseController;
import com.sp.common.core.domain.AjaxResult;
import com.sp.system.entity.SysSchoolDepartment;
import com.sp.system.service.ISysSchoolDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学校部门信息管理
 *
 */
@RestController
@RequestMapping("/system/schoolDepartment")
public class SchoolDepartmentController extends BaseController {

    @Autowired
    private ISysSchoolDepartmentService schoolDepartmentService;

    /**
     * 获取学校部门树形结构（带成员，用于教职员工选择器）
     */
    @Anonymous
    @GetMapping("/treeWithMembers")
    public AjaxResult treeWithMembers() {
        List<SysSchoolDepartment> tree = schoolDepartmentService.getSchoolDepartmentTreeWithMembers();
        return AjaxResult.success(tree);
    }

    /**
     * 获取学校部门树形结构（仅部门，不含人员）
     */
    @Anonymous
    @GetMapping("/tree")
    public AjaxResult tree() {
        List<SysSchoolDepartment> tree = schoolDepartmentService.getSchoolDepartmentTree();
        return AjaxResult.success(tree);
    }
}
