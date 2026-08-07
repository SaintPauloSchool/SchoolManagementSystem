package com.sms.system.service;

import com.sms.system.entity.dto.AttendanceRecordQueryDTO;
import com.sms.system.entity.vo.AttendanceRecordListVO;

import java.util.List;

/**
 * 考勤記錄 Service 接口
 */
public interface IAttendanceRecordService {

    /**
     * 查詢考勤機記錄列表
     */
    List<AttendanceRecordListVO> selectAttendanceRecordList(AttendanceRecordQueryDTO queryDTO);
}
