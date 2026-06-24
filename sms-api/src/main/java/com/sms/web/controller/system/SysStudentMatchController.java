package com.sms.web.controller.system;

import com.sms.common.annotation.Log;
import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.enums.BusinessType;
import com.sms.handler.system.StudentMatchHandler;
import com.sms.system.entity.query.SysStudentMatchQuery;
import com.sms.system.entity.vo.SysStudentMatchVO;
import com.sms.system.entity.vo.SysWecomStudentVO;
import com.sms.system.service.ISysStudentMatchService;
import com.sms.system.service.ISysAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 學生數據匹配與更名 控制層
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

    @GetMapping("/list")
    public TableDataInfo list(SysStudentMatchQuery query) {
        if (isNotAdmin()) {
            return getDataTable(new ArrayList<>());
        }
        startPage();
        List<SysStudentMatchVO> list = sysStudentMatchService.selectSysStudentMatchList(query);
        return getDataTable(list);
    }

    @GetMapping("/unmatchedList")
    public TableDataInfo unmatchedList(SysStudentMatchQuery query) {
        if (isNotAdmin()) {
            return getDataTable(new ArrayList<>());
        }
        startPage();
        List<SysStudentMatchVO> list = sysStudentMatchService.selectUnmatchedList(query);
        return getDataTable(list);
    }

    @GetMapping("/wecomCandidates")
    public TableDataInfo wecomCandidates(
            @RequestParam(value = "queryName", required = false) String queryName,
            @RequestParam(value = "queryMobile", required = false) String queryMobile,
            @RequestParam(value = "queryClass", required = false) String queryClass) {
        if (isNotAdmin()) {
            return getDataTable(new ArrayList<>());
        }
        startPage();
        List<SysWecomStudentVO> list = sysStudentMatchService.selectWecomCandidates(queryName, queryMobile, queryClass);
        return getDataTable(list);
    }

    @Log(title = "手動綁定學生匹配", businessType = BusinessType.UPDATE)
    @PostMapping("/bind")
    public AjaxResult bind(@RequestBody Map<String, Object> params) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        Long matchId = params.get("matchId") != null ? Long.valueOf(params.get("matchId").toString()) : null;
        String studentProfileNum = params.get("studentProfileNum") != null ? params.get("studentProfileNum").toString() : null;
        String studentUserIdWecom = params.get("studentUserIdWecom") != null ? params.get("studentUserIdWecom").toString() : null;

        if (studentUserIdWecom == null || (matchId == null && (studentProfileNum == null || studentProfileNum.isEmpty()))) {
            return AjaxResult.error("參數錯誤，請確認 matchId / studentProfileNum 和 studentUserIdWecom 是否為空");
        }

        boolean success = sysStudentMatchService.bindStudent(matchId, studentProfileNum, studentUserIdWecom);
        return success ? AjaxResult.success("綁定成功") : AjaxResult.error("綁定失敗，數據不存在");
    }

    @Log(title = "同步姓名至企業微信", businessType = BusinessType.UPDATE)
    @PostMapping("/sync")
    public AjaxResult sync(@RequestBody Map<String, Object> params) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        List<?> rawIds = (List<?>) params.get("matchIds");
        if (rawIds == null || rawIds.isEmpty()) {
            return AjaxResult.error("請選擇要同步的匹配記錄！");
        }

        List<Long> matchIds = new ArrayList<>();
        for (Object id : rawIds) {
            matchIds.add(Long.valueOf(id.toString()));
        }

        Map<String, Object> result = studentMatchHandler.syncStudentNames(matchIds, getUsername());
        return AjaxResult.success(result.get("message").toString(), result);
    }

    @Log(title = "同步對照數據", businessType = BusinessType.UPDATE)
    @PostMapping("/syncData")
    public AjaxResult syncData() {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        try {
            String msg = sysStudentMatchService.syncData(getUsername());
            return AjaxResult.success(msg);
        } catch (Exception e) {
            return AjaxResult.error("數據同步比對失敗：" + e.getMessage());
        }
    }

    @Log(title = "批量刪除學生匹配記錄", businessType = BusinessType.DELETE)
    @PostMapping("/delete")
    public AjaxResult delete(@RequestBody Map<String, Object> params) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        List<?> rawIds = (List<?>) params.get("matchIds");
        if (rawIds == null || rawIds.isEmpty()) {
            return AjaxResult.error("請選擇要刪除的匹配記錄！");
        }
        List<Long> ids = new ArrayList<>();
        for (Object id : rawIds) {
            ids.add(Long.valueOf(id.toString()));
        }
        int rows = sysStudentMatchService.deleteSysStudentMatchByIds(ids);
        return rows > 0 ? AjaxResult.success("刪除成功") : AjaxResult.error("刪除失敗");
    }

    @Log(title = "清除學生匹配關係", businessType = BusinessType.UPDATE)
    @PostMapping("/clear")
    public AjaxResult clear(@RequestBody Map<String, Object> params) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        Long matchId = params.get("matchId") != null ? Long.valueOf(params.get("matchId").toString()) : null;
        String studentProfileNum = params.get("studentProfileNum") != null ? params.get("studentProfileNum").toString() : null;
        if (matchId == null && (studentProfileNum == null || studentProfileNum.isEmpty())) {
            return AjaxResult.error("參數錯誤，請確認 matchId 或 studentProfileNum 是否為空");
        }

        boolean success = sysStudentMatchService.clearMatch(matchId, studentProfileNum);
        return success ? AjaxResult.success("清除匹配成功") : AjaxResult.error("清除匹配失敗，數據不存在");
    }
}
