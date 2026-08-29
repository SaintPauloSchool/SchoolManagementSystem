package com.sms.web.controller.system;

import com.sms.common.annotation.Log;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.enums.BusinessType;
import com.sms.common.utils.poi.ExcelUtil;
import com.sms.system.entity.dto.AttendanceRecordQueryDTO;
import com.sms.system.entity.vo.AttendanceRecordListVO;
import com.sms.system.service.IAttendanceRecordService;
import com.sms.web.controller.base.SysUserRoleBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 考勤機記錄查詢
 */
@RestController
@RequestMapping("/system/student/attendance")
public class AttendanceRecordController extends SysUserRoleBaseController {

    @Autowired
    private IAttendanceRecordService attendanceRecordService;

    @Log(title = "查詢考勤機記錄列表", businessType = BusinessType.SELECT)
    @GetMapping("/list")
    public TableDataInfo list(AttendanceRecordQueryDTO queryDTO) {
        if (isNotUserRole()) {
            return getDataTable(new ArrayList<>());
        }
        startPage();
        List<AttendanceRecordListVO> list = attendanceRecordService.selectAttendanceRecordList(queryDTO);
        return getDataTable(list);
    }

    @Log(title = "導出考勤機記錄", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public void export(AttendanceRecordQueryDTO queryDTO, HttpServletResponse response) throws Exception {
        if (isNotUserRole()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        List<AttendanceRecordListVO> list = attendanceRecordService.selectAttendanceRecordList(queryDTO);
        String filename = URLEncoder.encode("考勤機記錄.xlsx", StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        ExcelUtil<AttendanceRecordListVO> util = new ExcelUtil<>(AttendanceRecordListVO.class);
        util.exportExcel(response, list, "考勤機記錄");
    }
}
