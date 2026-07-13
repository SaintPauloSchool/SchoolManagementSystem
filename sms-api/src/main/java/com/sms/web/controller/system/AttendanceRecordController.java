package com.sms.web.controller.system;

import com.sms.common.annotation.Log;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.enums.BusinessType;
import com.sms.system.entity.dto.AttendanceRecordQueryDTO;
import com.sms.system.entity.vo.AttendanceRecordListVO;
import com.sms.system.service.IAttendanceRecordService;
import com.sms.web.controller.base.AdminBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 考勤機記錄查詢
 */
@RestController
@RequestMapping("/system/student/attendance")
public class AttendanceRecordController extends AdminBaseController {

    @Autowired
    private IAttendanceRecordService attendanceRecordService;

    @Log(title = "查詢考勤機記錄列表", businessType = BusinessType.SELECT)
    @GetMapping("/list")
    public TableDataInfo list(AttendanceRecordQueryDTO queryDTO) {
        if (isNotAdmin()) {
            return getDataTable(new ArrayList<>());
        }
        startPage();
        List<AttendanceRecordListVO> list = attendanceRecordService.selectAttendanceRecordList(queryDTO);
        return getDataTable(list);
    }
}
