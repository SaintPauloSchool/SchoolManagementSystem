package com.sms.system.service.impl;

import com.sms.common.config.StudentProfilesProperties;
import com.sms.system.entity.dto.AttendanceRecordQueryDTO;
import com.sms.system.entity.vo.AttendanceRecordListVO;
import com.sms.system.mapper.AttendanceRecordMapper;
import com.sms.system.service.IAttendanceRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 考勤記錄 Service 實現類
 */
@Service
public class AttendanceRecordServiceImpl implements IAttendanceRecordService {

    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;

    @Autowired
    private StudentProfilesProperties studentProfilesProperties;

    @Override
    public List<AttendanceRecordListVO> selectAttendanceRecordList(AttendanceRecordQueryDTO queryDTO) {
        return attendanceRecordMapper.selectAttendanceRecordList(queryDTO, studentProfilesProperties.getDatabase());
    }
}
