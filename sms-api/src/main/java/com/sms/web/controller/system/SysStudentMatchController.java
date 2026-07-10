package com.sms.web.controller.system;

import com.sms.common.annotation.Log;
import com.sms.web.controller.base.AdminBaseController;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.enums.BusinessType;
import com.sms.system.entity.dto.SysStudentMatchBatchBindDTO;
import com.sms.system.entity.dto.SysStudentMatchDTO;
import com.sms.system.entity.dto.SysStudentMatchSyncDataDTO;
import com.sms.system.entity.dto.SysStudentMatchUpdateDTO;
import com.sms.system.entity.dto.SysWecomStudentDTO;
import com.sms.system.entity.vo.SysSchoolFamilyContactVO;
import com.sms.system.entity.vo.SysStudentMatchOperationResultVO;
import com.sms.system.entity.vo.SysStudentMatchVO;
import com.sms.system.entity.vo.StudentPhotoVO;
import com.sms.system.service.IStudentPhotoService;
import com.sms.system.service.ISysStudentMatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 學生數據匹配
 */
@RestController
@RequestMapping("/system/student/match")
public class SysStudentMatchController extends AdminBaseController {

    @Autowired
    private ISysStudentMatchService sysStudentMatchService;

    @Autowired
    private IStudentPhotoService studentPhotoService;

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
        List<SysSchoolFamilyContactVO> wecomCandidateList = sysStudentMatchService.selectWecomCandidates(wecomStudentDTO);
        return getDataTable(wecomCandidateList);
    }

    @Log(title = "獲取學生照片", businessType = BusinessType.SELECT)
    @GetMapping("/photo/{studentProfileNumber}")
    public void photo(@PathVariable("studentProfileNumber") String studentProfileNumber, HttpServletResponse response)
            throws IOException {
        if (isNotAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        StudentPhotoVO photoVO = studentPhotoService.fetchPhoto(studentProfileNumber);
        if (photoVO == null || photoVO.getData() == null || photoVO.getData().length == 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(photoVO.getContentType());
        response.setHeader("Cache-Control", "private, max-age=3600");
        response.getOutputStream().write(photoVO.getData());
    }

    @Log(title = "手動綁定學生匹配", businessType = BusinessType.UPDATE)
    @PostMapping("/bindBatch")
    public AjaxResult bindBatch(@RequestBody SysStudentMatchBatchBindDTO batchBindDTO) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        SysStudentMatchOperationResultVO resultVO = sysStudentMatchService.bindStudents(batchBindDTO);
        return AjaxResult.from(resultVO.isSuccess(), resultVO.getMessage());
    }

    @Log(title = "更正學生匹配家長資訊", businessType = BusinessType.UPDATE)
    @PutMapping("/update")
    public AjaxResult update(@RequestBody SysStudentMatchUpdateDTO updateDTO) {
        if (isNotAdmin()) {
            return AjaxResult.error("無權限訪問");
        }
        SysStudentMatchOperationResultVO resultVO = sysStudentMatchService.updateStudentMatch(updateDTO);
        return AjaxResult.from(resultVO.isSuccess(), resultVO.getMessage());
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
}
