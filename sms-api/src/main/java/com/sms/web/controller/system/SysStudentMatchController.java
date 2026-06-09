package com.sms.web.controller.system;

import com.sms.common.annotation.Log;
import com.sms.common.core.controller.BaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.enums.BusinessType;
import com.sms.handler.system.StudentMatchHandler;
import com.sms.system.entity.SysStudentMatch;
import com.sms.system.entity.vo.SysWecomStudentVO;
import com.sms.system.service.ISysStudentMatchService;
import com.sms.system.service.ISysAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 學生數據匹配與更名 控制層
 *
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

    /**
     * 校驗是否為非管理員
     */
    private boolean isNotAdmin() {
        return sysAdminService.isNotAdmin(getOpenUserId());
    }

    /**
     * 查詢學生數據匹配列表 (分頁)
     */
    @GetMapping("/list")
    public TableDataInfo list(SysStudentMatch sysStudentMatch) {
        if (isNotAdmin()) {
            return getDataTable(new ArrayList<>());
        }
        startPage();
        List<SysStudentMatch> list = sysStudentMatchService.selectSysStudentMatchList(sysStudentMatch);
        return getDataTable(list);
    }

    /**
     * 獲取未匹配本地數據列表 (彈窗 1)
     */
    @GetMapping("/unmatchedList")
    public TableDataInfo unmatchedList(SysStudentMatch sysStudentMatch) {
        if (isNotAdmin()) {
            return getDataTable(new ArrayList<>());
        }
        startPage();
        List<SysStudentMatch> list = sysStudentMatchService.selectUnmatchedList(sysStudentMatch);
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

    /**
     * 導入 Excel 學籍數據並執行自動匹配
     */
    @Log(title = "導入學籍匹配數據", businessType = BusinessType.IMPORT)
    @PostMapping("/import")
    public AjaxResult importExcel(MultipartFile file) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        try {
            String message = sysStudentMatchService.importExcel(file, getUsername());
            return AjaxResult.success(message);
        } catch (Exception e) {
            return AjaxResult.error("導入失敗：" + e.getMessage());
        }
    }

    /**
     * 手動綁定學生匹配關係
     */
    @Log(title = "手動綁定學生匹配", businessType = BusinessType.UPDATE)
    @PostMapping("/bind")
    public AjaxResult bind(@RequestBody Map<String, Object> params) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        Long matchId = params.get("matchId") != null ? Long.valueOf(params.get("matchId").toString()) : null;
        String studentUserIdWecom = params.get("studentUserIdWecom") != null ? params.get("studentUserIdWecom").toString() : null;
        
        if (matchId == null || studentUserIdWecom == null) {
            return AjaxResult.error("參數錯誤，請確認 matchId 和 studentUserIdWecom 是否為空");
        }
        
        boolean success = sysStudentMatchService.bindStudent(matchId, studentUserIdWecom);
        return success ? AjaxResult.success("綁定成功") : AjaxResult.error("綁定失敗，數據不存在");
    }

    /**
     * 確定匹配並批量同步更名至企業微信
     */
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

    /**
     * 同步/匹配數據 (本地數據比對匹配)
     */
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

    /**
     * 下載學籍導入模板
     */
    @GetMapping("/importTemplate")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        if (isNotAdmin()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        sysStudentMatchService.downloadTemplate(response);
    }
}
