package com.sms.web.controller.system;

import com.sms.common.annotation.Log;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.enums.BusinessType;
import com.sms.system.entity.dto.AddressBookSegmentSettingDTO;
import com.sms.system.entity.dto.DailyNoticeClassSettingDTO;
import com.sms.system.entity.vo.SysDepartmentVO;
import com.sms.system.service.ISysConfigService;
import com.sms.system.service.ISysDepartmentService;
import com.sms.web.controller.base.AdminBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 基本設置
 */
@RestController
@RequestMapping("/system/basic")
public class SysBasicSettingController extends AdminBaseController {

    @Autowired
    private ISysDepartmentService departmentService;

    @Autowired
    private ISysConfigService sysConfigService;

    /** 學段樹（僅到 type=3，供選擇） */
    @Log(title = "查詢學段樹", businessType = BusinessType.SELECT)
    @GetMapping("/addressBook/segmentTree")
    public AjaxResult segmentTree() {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限");
        }
        List<SysDepartmentVO> tree = departmentService.getSegmentTree();
        return AjaxResult.success(tree);
    }

    /** 讀取家校通訊錄學段設置 */
    @Log(title = "查詢家校通訊錄學段設置", businessType = BusinessType.SELECT)
    @GetMapping("/addressBook/segmentSetting")
    public AjaxResult getAddressBookSegmentSetting() {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限");
        }
        AddressBookSegmentSettingDTO dto = new AddressBookSegmentSettingDTO();
        dto.setSegmentDepartmentId(sysConfigService.getAddressBookSegmentDepartmentId());
        return AjaxResult.success(dto);
    }

    /** 保存家校通訊錄學段設置 */
    @Log(title = "保存家校通訊錄學段設置", businessType = BusinessType.UPDATE)
    @PostMapping("/addressBook/segmentSetting")
    public AjaxResult saveAddressBookSegmentSetting(@RequestBody AddressBookSegmentSettingDTO settingDTO) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限");
        }
        sysConfigService.saveAddressBookSegmentDepartmentId(
                settingDTO != null ? settingDTO.getSegmentDepartmentId() : null,
                getOpenUserId());
        return AjaxResult.success("保存成功");
    }

    /** 每日學生手冊通知班級選擇樹（含 type=1） */
    @Log(title = "查詢每日通知班級樹", businessType = BusinessType.SELECT)
    @GetMapping("/dailyNotice/classTree")
    public AjaxResult dailyNoticeClassTree() {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限");
        }
        List<SysDepartmentVO> tree = departmentService.getDailyNoticeClassTree();
        return AjaxResult.success(tree);
    }

    /** 讀取每日學生手冊通知班級範圍設置 */
    @Log(title = "查詢每日通知班級設置", businessType = BusinessType.SELECT)
    @GetMapping("/dailyNotice/classSetting")
    public AjaxResult getDailyNoticeClassSetting() {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限");
        }
        DailyNoticeClassSettingDTO dto = new DailyNoticeClassSettingDTO();
        dto.setClassDepartmentIds(sysConfigService.getDailyNoticeClassDepartmentIds());
        return AjaxResult.success(dto);
    }

    /** 保存每日學生手冊通知班級範圍設置 */
    @Log(title = "保存每日通知班級設置", businessType = BusinessType.UPDATE)
    @PostMapping("/dailyNotice/classSetting")
    public AjaxResult saveDailyNoticeClassSetting(@RequestBody DailyNoticeClassSettingDTO settingDTO) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限");
        }
        List<Long> ids = settingDTO != null ? settingDTO.getClassDepartmentIds() : null;
        sysConfigService.saveDailyNoticeClassDepartmentIds(ids, getOpenUserId());
        return AjaxResult.success("保存成功");
    }
}
