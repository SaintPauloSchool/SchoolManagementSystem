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
     * 獲取班級樹形結構
     */
    @Anonymous
    @GetMapping("/tree")
    public AjaxResult tree() {
        List<SysDepartment> tree = departmentService.getClassTree();
        return AjaxResult.success(tree);
    }

    /**
     * 獲取班級樹形結構（帶家長學生關係，用於學生/家長選擇器）
     */
    @Anonymous
    @GetMapping("/treeWithParents")
    public AjaxResult treeWithParents() {
        List<SysDepartment> tree = departmentService.getClassTreeWithParents();
        return AjaxResult.success(tree);
    }

}
