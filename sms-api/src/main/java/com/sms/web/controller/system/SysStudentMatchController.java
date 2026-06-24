package com.sms.web.controller.system;

import com.sms.common.annotation.Log;
import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.enums.BusinessType;
import com.sms.handler.system.StudentMatchHandler;
import com.sms.system.entity.dto.SysStudentMatchBindDTO;
import com.sms.system.entity.dto.SysStudentMatchClearDTO;
import com.sms.system.entity.dto.SysStudentMatchDTO;
import com.sms.system.entity.dto.SysStudentMatchDeleteDTO;
import com.sms.system.entity.dto.SysStudentMatchSyncDataDTO;
import com.sms.system.entity.dto.SysStudentMatchSyncDTO;
import com.sms.system.entity.dto.SysWecomStudentDTO;
import com.sms.system.entity.vo.SysStudentMatchOperationResultVO;
import com.sms.system.entity.vo.SysStudentMatchSyncResultVO;
import com.sms.system.entity.vo.SysStudentMatchVO;
import com.sms.system.entity.vo.SysWecomStudentVO;
import com.sms.system.service.ISysAdminService;
import com.sms.system.service.ISysStudentMatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 學生數據匹配與更名 控制層
 * <p>入參：DTO；出參：VO</p>
 */
@RestController
@RequestMapping("/system/student/match")
public class SysStudentMatchController extends BaseController {

    @Autowired
    private ISysStudentMatchService sysStudentMatchService;

    @Autowired
    private ISysAdminService sysAdminService;

    @Autowired
    private StudentMatchHandler studentMatchHandler;

    private boolean isNotAdmin() {
        return sysAdminService.isNotAdmin(getOpenUserId());
    }

    @Log(title = "查詢學生匹配列表", businessType = BusinessType.SELECT)
    @GetMapping("/list")
    public TableDataInfo list(SysStudentMatchDTO studentMatchDTO) {
        if (isNotAdmin()) {
            return getDataTable(new ArrayList<>());
        }
        startPage();
        List<SysStudentMatchVO> studentMatchList = sysStudentMatchService.selectSysStudentMatchList(studentMatchDTO);
        return getDataTable(studentMatchList);
    }

    @Log(title = "查詢未匹配學生列表", businessType = BusinessType.SELECT)
    @GetMapping("/unmatchedList")
    public TableDataInfo unmatchedList(SysStudentMatchDTO studentMatchDTO) {
        if (isNotAdmin()) {
            return getDataTable(new ArrayList<>());
        }
        startPage();
        List<SysStudentMatchVO> studentMatchList = sysStudentMatchService.selectUnmatchedList(studentMatchDTO);
        return getDataTable(studentMatchList);
    }

    @Log(title = "查詢企微學生候選列表", businessType = BusinessType.SELECT)
    @GetMapping("/wecomCandidates")
    public TableDataInfo wecomCandidates(SysWecomStudentDTO wecomStudentDTO) {
        if (isNotAdmin()) {
            return getDataTable(new ArrayList<>());
        }
        startPage();
        List<SysWecomStudentVO> wecomStudentList = sysStudentMatchService.selectWecomCandidates(wecomStudentDTO);
        return getDataTable(wecomStudentList);
    }

    @Log(title = "手動綁定學生匹配", businessType = BusinessType.UPDATE)
    @PostMapping("/bind")
    public AjaxResult bind(@RequestBody SysStudentMatchBindDTO studentMatchBindDTO) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        SysStudentMatchOperationResultVO resultVO = sysStudentMatchService.bindStudent(studentMatchBindDTO);
        return AjaxResult.from(resultVO.isSuccess(), resultVO.getMessage());
    }

    @Log(title = "同步姓名至企業微信", businessType = BusinessType.UPDATE)
    @PostMapping("/sync")
    public AjaxResult sync(@RequestBody SysStudentMatchSyncDTO studentMatchSyncDTO) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        studentMatchSyncDTO.setOperName(getUsername());
        SysStudentMatchSyncResultVO resultVO = studentMatchHandler.syncStudentNames(studentMatchSyncDTO);
        return AjaxResult.success(resultVO.getMessage(), resultVO);
    }

    @Log(title = "同步對照數據", businessType = BusinessType.UPDATE)
    @PostMapping("/syncData")
    public AjaxResult syncData() {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        try {
            SysStudentMatchSyncDataDTO syncDataDTO = new SysStudentMatchSyncDataDTO();
            syncDataDTO.setOperName(getUsername());
            SysStudentMatchOperationResultVO resultVO = sysStudentMatchService.syncData(syncDataDTO);
            return AjaxResult.from(resultVO.isSuccess(), resultVO.getMessage());
        } catch (Exception e) {
            return AjaxResult.error("數據同步比對失敗：" + e.getMessage());
        }
    }

    @Log(title = "批量刪除學生匹配記錄", businessType = BusinessType.DELETE)
    @PostMapping("/delete")
    public AjaxResult delete(@RequestBody SysStudentMatchDeleteDTO studentMatchDeleteDTO) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        SysStudentMatchOperationResultVO resultVO = sysStudentMatchService.deleteSysStudentMatchByIds(studentMatchDeleteDTO);
        return AjaxResult.from(resultVO.isSuccess(), resultVO.getMessage());
    }

    @Log(title = "清除學生匹配關係", businessType = BusinessType.UPDATE)
    @PostMapping("/clear")
    public AjaxResult clear(@RequestBody SysStudentMatchClearDTO studentMatchClearDTO) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        SysStudentMatchOperationResultVO resultVO = sysStudentMatchService.clearMatch(studentMatchClearDTO);
        return AjaxResult.from(resultVO.isSuccess(), resultVO.getMessage());
    }
}
