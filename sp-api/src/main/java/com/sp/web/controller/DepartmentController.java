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

    /**
     * 查询所有班级列表（type=1）
     */
    @Anonymous
    @GetMapping("/classes")
    public AjaxResult classes() {
        List<SysDepartment> classes = departmentService.selectAllClasses();
        return AjaxResult.success(classes);
    }

    /**
     * 根据类型查询部门列表
     */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam Integer type) {
        List<SysDepartment> list = departmentService.selectByType(type);
        return AjaxResult.success(list);
    }

    /**
     * 根据父级 ID 查询子部门
     */
    @GetMapping("/children")
    public AjaxResult children(@RequestParam Integer parentId) {
        List<SysDepartment> list = departmentService.selectByParentId(parentId);
        return AjaxResult.success(list);
    }
}
