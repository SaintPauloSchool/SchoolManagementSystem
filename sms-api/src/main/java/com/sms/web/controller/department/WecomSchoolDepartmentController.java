package com.sms.web.controller.department;

import com.sms.common.annotation.Log;
import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.enums.BusinessType;
import com.sms.system.entity.vo.WecomSchoolDepartmentVO;
import com.sms.system.service.IWecomSchoolDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * WeCom學校部門資訊管理
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
    @Log(title = "查詢學校部門樹（含成員）", businessType = BusinessType.SELECT)
    @GetMapping("/treeWithMembers")
    public AjaxResult treeWithMembers() {
        List<WecomSchoolDepartmentVO> tree = wecomSchoolDepartmentService.getWecomSchoolDepartmentTreeWithMembers();
        return AjaxResult.success(tree);
    }
}
