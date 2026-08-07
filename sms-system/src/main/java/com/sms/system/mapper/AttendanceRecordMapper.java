package com.sms.system.mapper;

import com.sms.system.entity.dto.AttendanceRecordQueryDTO;
import com.sms.system.entity.vo.AttendanceNotifyRowVO;
import com.sms.system.entity.vo.AttendanceRecordListVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考勤記錄 Mapper
 */
public interface AttendanceRecordMapper {

    /**
     * 查詢待通知的考勤記錄（關聯學籍與家長匹配）
     */
    List<AttendanceNotifyRowVO> selectPendingNotifyRows(
            @Param("studentProfilesDatabase") String studentProfilesDatabase,
            @Param("limit") int limit);

    /**
     * 查詢考勤機記錄列表（關聯學籍）
     */
    List<AttendanceRecordListVO> selectAttendanceRecordList(
            @Param("query") AttendanceRecordQueryDTO query,
            @Param("studentProfilesDatabase") String studentProfilesDatabase);

    /**
     * 標記考勤記錄為已通知
     */
    int markNotified(@Param("id") Long id);
}
