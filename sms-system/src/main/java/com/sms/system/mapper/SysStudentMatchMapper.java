package com.sms.system.mapper;

import com.sms.system.entity.SysStudentMatch;
import com.sms.system.entity.dto.SysStudentMatchDTO;
import com.sms.system.entity.vo.SysStudentMatchVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 學生數據匹配 Mapper 介面
 */
public interface SysStudentMatchMapper {

    /**
     * 查詢學生匹配列表（關聯學生檔案庫與匹配表）
     */
    List<SysStudentMatchVO> selectSysStudentMatchList(
            @Param("studentMatchDTO") SysStudentMatchDTO studentMatchDTO,
            @Param("studentProfilesDatabase") String studentProfilesDatabase);

    /**
     * 查詢未匹配學生列表
     */
    List<SysStudentMatchVO> selectUnmatchedList(
            @Param("studentMatchDTO") SysStudentMatchDTO studentMatchDTO,
            @Param("studentProfilesDatabase") String studentProfilesDatabase);

    /**
     * 查詢全部學籍學生（用於自動匹配，每位學生一條）
     */
    List<SysStudentMatchVO> selectAllStudentsForMatch(
            @Param("studentProfilesDatabase") String studentProfilesDatabase);

    /**
     * 批量保存已匹配記錄
     */
    int batchSaveOrUpdateStudentMatch(@Param("matches") List<SysStudentMatch> matches);

    SysStudentMatch selectStudentMatchById(@Param("id") Long id);

    int countStudentMatchByStudentAndUserExceptId(
            @Param("studentId") String studentId,
            @Param("userId") String userId,
            @Param("excludeId") Long excludeId);

    int updateStudentMatchUserId(SysStudentMatch studentMatch);

}
