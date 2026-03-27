package com.sp.web.controller;

import com.sp.common.annotation.Anonymous;
import com.sp.common.core.controller.BaseController;
import com.sp.common.core.domain.AjaxResult;
import com.sp.system.entity.SysDepartment;
import com.sp.system.service.ISysDepartmentService;
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
}
