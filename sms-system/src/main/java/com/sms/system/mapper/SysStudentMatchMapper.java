package com.sms.system.mapper;

import com.sms.system.entity.SysStudentMatch;
import com.sms.system.entity.query.SysStudentMatchQuery;
import com.sms.system.entity.vo.SysStudentMatchVO;
import com.sms.system.entity.vo.SysWecomStudentVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 學生數據匹配 Mapper 介面
 */
public interface SysStudentMatchMapper {

    /**
     * 查詢學生數據匹配列表（關聯 student_profiles.student_info）
     */
    List<SysStudentMatchVO> selectSysStudentMatchList(
            @Param("query") SysStudentMatchQuery query,
            @Param("studentProfilesDatabase") String studentProfilesDatabase);

    /**
     * 根據 ID 查詢學生數據匹配詳情（含學籍資料）
     */
    SysStudentMatchVO selectStudentMatchVOById(
            @Param("id") Long id,
            @Param("studentProfilesDatabase") String studentProfilesDatabase);

    /**
     * 根據 ID 查詢匹配記錄（僅 sys_student_match 表）
     */
    SysStudentMatch selectMatchById(@Param("id") Long id);

    /**
     * 根據學生個人編號查詢匹配記錄 ID
     */
    Long selectMatchIdByProfileNum(@Param("studentProfileNum") String studentProfileNum);

    /**
     * 新增匹配記錄（僅寫入 student_profile_num）
     */
    int insertMatchRecord(@Param("studentProfileNum") String studentProfileNum);

    /**
     * 修改學生數據匹配
     */
    int updateSysStudentMatch(SysStudentMatch sysStudentMatch);

    /**
     * 查詢未匹配的學籍數據列表
     */
    List<SysStudentMatchVO> selectUnmatchedList(
            @Param("query") SysStudentMatchQuery query,
            @Param("studentProfilesDatabase") String studentProfilesDatabase);

    /**
     * 查詢企微學生手動匹配候選名單
     */
    List<SysWecomStudentVO> selectWecomCandidates(
        @Param("queryNameTraditional") String queryNameTraditional,
        @Param("queryNameSimplified") String queryNameSimplified,
        @Param("queryMobile") String queryMobile,
        @Param("queryClass") String queryClass
    );

    /**
     * 獲取本地關係表中所有與班級相關聯的企微學生列表，用於自動匹配比對
     */
    List<SysWecomStudentVO> selectWecomStudentInfoList();

    /**
     * 更新企微學生的本地關係表姓名
     */
    int updateWecomStudentName(@Param("studentUserId") String studentUserId, @Param("studentName") String studentName);

    /**
     * 批量刪除學生匹配記錄
     */
    int deleteSysStudentMatchByIds(@Param("ids") List<Long> ids);

    /**
     * 清除單個匹配狀態
     */
    int clearStudentMatch(@Param("id") Long id);
}
