package com.sms.system.mapper;

import com.sms.system.entity.vo.AttendanceNotifyRowVO;
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
     * 標記考勤記錄為已通知
     */
    int markNotified(@Param("id") Long id);
}
