package com.sms.web.controller;

import com.sms.common.annotation.Log;
import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.enums.BusinessType;
import com.sms.system.entity.vo.SysDepartmentVO;
import com.sms.system.service.ISysDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部門信息管理
 */
@RestController
@RequestMapping("/system/department")
public class DepartmentController extends BaseController {

    @Autowired
    private ISysDepartmentService departmentService;

    /**
     * 獲取家校通訊錄樹（帶家長學生關係，用於學生/家長選擇器、家校通訊錄）
     */
    @Log(title = "查詢家校通訊錄樹", businessType = BusinessType.SELECT)
    @GetMapping("/treeWithParents")
    public AjaxResult treeWithParents() {
        String openUserId = getOpenUserId();
        List<SysDepartmentVO> tree = departmentService.getClassTreeWithParentsByAdmin(openUserId);
        return AjaxResult.success(tree);
    }
}
