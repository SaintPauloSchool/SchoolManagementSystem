package com.sms.web.controller.system;

import com.sms.common.annotation.Log;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.enums.BusinessType;
import com.sms.system.entity.ClassSection;
import com.sms.system.entity.dto.ClassSectionQueryDTO;
import com.sms.system.entity.dto.ClassSectionSaveDTO;
import com.sms.system.service.IClassSectionService;
import com.sms.web.controller.base.AdminBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 班級對照管理（基礎設置）
 */
@RestController
@RequestMapping("/system/basic/classSection")
public class ClassSectionController extends AdminBaseController {

    @Autowired
    private IClassSectionService classSectionService;

    @Log(title = "查詢班級對照列表", businessType = BusinessType.SELECT)
    @GetMapping("/list")
    public TableDataInfo list(ClassSectionQueryDTO queryDTO) {
        if (isNotAdmin()) {
            return getDataTable(new ArrayList<>());
        }
        startPage();
        List<ClassSection> list = classSectionService.selectClassSectionList(queryDTO);
        return getDataTable(list);
    }

    @Log(title = "查詢班級對照詳情", businessType = BusinessType.SELECT)
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        return AjaxResult.success(classSectionService.selectClassSectionById(id));
    }

    @Log(title = "新增班級對照", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ClassSectionSaveDTO saveDTO) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        return toAjax(classSectionService.insertClassSection(saveDTO));
    }

    @Log(title = "修改班級對照", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ClassSectionSaveDTO saveDTO) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        return toAjax(classSectionService.updateClassSection(saveDTO));
    }

    @Log(title = "刪除班級對照", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        return toAjax(classSectionService.deleteClassSectionByIds(ids));
    }
}
