package com.sms.system.mapper;

import com.sms.system.entity.SysStudentMatch;
import com.sms.system.entity.vo.SysWecomStudentVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 學生數據匹配 Mapper 介面
 *
 */
public interface SysStudentMatchMapper {

    /**
     * 查詢學生數據匹配對象列表
     */
    List<SysStudentMatch> selectSysStudentMatchList(SysStudentMatch sysStudentMatch);

    /**
     * 根據 ID 查詢學生數據匹配詳情
     */
    SysStudentMatch selectSysStudentMatchById(Long id);

    /**
     * 修改學生數據匹配
     */
    int updateSysStudentMatch(SysStudentMatch sysStudentMatch);

    /**
     * 批量寫入或更新學生數據匹配 (Upsert 邏輯)
     */
    int batchInsertOrUpdate(@Param("list") List<SysStudentMatch> list);

    /**
     * 查詢未匹配的 Excel 導入數據
     */
    List<SysStudentMatch> selectUnmatchedList(SysStudentMatch sysStudentMatch);

    List<SysWecomStudentVO> selectWecomCandidates(
        @Param("queryNameTraditional") String queryNameTraditional,
        @Param("queryNameSimplified") String queryNameSimplified,
        @Param("queryMobile") String queryMobile,
        @Param("queryClass") String queryClass
    );

    /**
     * 獲取本地關係表中所有與班級相關聯 of 企微學生列表，用於班級+姓名自動匹配比對
     */
    List<SysWecomStudentVO> selectWecomStudentInfoList();
    
    /**
     * 更新企微學生的本地關係表姓名
     */
    int updateWecomStudentName(@Param("studentUserId") String studentUserId, @Param("studentName") String studentName);

    /**
     * 批量刪除學生數據匹配
     */
    int deleteSysStudentMatchByIds(@Param("ids") List<Long> ids);

    /**
     * 清除單個匹配狀態
     */
    int clearStudentMatch(@Param("id") Long id);

    /**
     * 清空所有對照數據
     */
    int deleteAllSysStudentMatch();
}
