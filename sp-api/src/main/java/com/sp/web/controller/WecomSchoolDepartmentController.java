package com.sp.web.controller;

import com.sp.common.annotation.Anonymous;
import com.sp.common.core.controller.BaseController;
import com.sp.common.core.domain.AjaxResult;
import com.sp.system.entity.WecomSchoolDepartment;
import com.sp.system.service.IWecomSchoolDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * WeCom学校部门信息管理
 *
 */
@RestController
@RequestMapping("/wecomSchoolDepartment")
public class WecomSchoolDepartmentController extends BaseController {

    @Autowired
    private IWecomSchoolDepartmentService wecomSchoolDepartmentService;

    /**
     * 獲取學校部門樹形結構（帶成員，用於教職員工選擇器）
     */
    @Anonymous
    @GetMapping("/treeWithMembers")
    public AjaxResult treeWithMembers() {
        List<WecomSchoolDepartment> tree = wecomSchoolDepartmentService.getWecomSchoolDepartmentTreeWithMembers();
        return AjaxResult.success(tree);
    }

    /**
     * 獲取學校部門樹形結構（僅部門，不含人員）
     */
    @Anonymous
    @GetMapping("/tree")
    public AjaxResult tree() {
        List<WecomSchoolDepartment> tree = wecomSchoolDepartmentService.getWecomSchoolDepartmentTree();
        return AjaxResult.success(tree);
    }
}
