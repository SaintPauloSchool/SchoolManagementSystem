package com.sms.web.controller;

import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.system.entity.SysDepartment;
import com.sms.system.service.ISysDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部門信息管理
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
    @GetMapping("/tree")
    public AjaxResult tree() {
        String openUserId = getOpenUserId();
        List<SysDepartment> tree = departmentService.getClassTreeByAdmin(openUserId);
        return AjaxResult.success(tree);
    }

    /**
     * 獲取班級樹形結構（帶家長學生關係，用於學生/家長選擇器）
     */
    @GetMapping("/treeWithParents")
    public AjaxResult treeWithParents() {
        String openUserId = getOpenUserId();
        List<SysDepartment> tree = departmentService.getClassTreeWithParentsByAdmin(openUserId);
        return AjaxResult.success(tree);
    }

}
