package com.sp.web.controller;

import com.sp.common.annotation.Anonymous;
import com.sp.common.core.controller.BaseController;
import com.sp.common.core.domain.AjaxResult;
import com.sp.system.entity.SysDepartment;
import com.sp.system.entity.SysSchoolDepartment;
import com.sp.system.service.ISysDepartmentService;
import com.sp.system.service.ISysSchoolDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门信息管理
 *
 */
@RestController
@RequestMapping("/system/department")
public class DepartmentController extends BaseController {

    @Autowired
    private ISysDepartmentService departmentService;

    @Autowired
    private ISysSchoolDepartmentService schoolDepartmentService;

    /**
     * 获取班级树形结构
     */
    @Anonymous
    @GetMapping("/tree")
    public AjaxResult tree() {
        List<SysDepartment> tree = departmentService.getClassTree();
        return AjaxResult.success(tree);
    }

    /**
     * 获取班级树形结构（带家长学生关系，用于学生/家长选择器）
     */
    @Anonymous
    @GetMapping("/treeWithParents")
    public AjaxResult treeWithParents() {
        List<SysDepartment> tree = departmentService.getClassTreeWithParents();
        return AjaxResult.success(tree);
    }

    /**
     * 获取学校部门树形结构（带成员，用于教职员工选择器）
     */
    @Anonymous
    @GetMapping("/treeWithMembers")
    public AjaxResult treeWithMembers() {
        List<SysSchoolDepartment> tree = schoolDepartmentService.getSchoolDepartmentTreeWithMembers();
        return AjaxResult.success(tree);
    }
}
