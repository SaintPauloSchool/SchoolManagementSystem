package com.sms.system.mapper;

import com.sms.system.entity.dto.SysStudentMatchDeleteDTO;
import com.sms.system.entity.dto.SysStudentMatchDeptQueryDTO;
import com.sms.system.entity.dto.SysStudentMatchDTO;
import com.sms.system.entity.dto.SysStudentMatchInsertDTO;
import com.sms.system.entity.dto.SysStudentMatchProfileNumDTO;
import com.sms.system.entity.dto.SysStudentMatchUpdateDTO;
import com.sms.system.entity.dto.SysWecomStudentDTO;
import com.sms.system.entity.dto.SysWecomStudentNameUpdateDTO;
import com.sms.system.entity.vo.SysStudentMatchDeptItemVO;
import com.sms.system.entity.vo.SysStudentMatchVO;
import com.sms.system.entity.vo.SysWecomStudentVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 學生數據匹配 Mapper 介面
 */
public interface SysStudentMatchMapper {

    /**
     * 查詢學生匹配列表（關聯學生檔案庫與匹配表）
     *
     * @param studentMatchDTO          查詢條件
     * @param studentProfilesDatabase  學生檔案庫名稱（跨庫查詢）
     * @return 學生匹配列表
     */
    List<SysStudentMatchVO> selectSysStudentMatchList(
            @Param("studentMatchDTO") SysStudentMatchDTO studentMatchDTO,
            @Param("studentProfilesDatabase") String studentProfilesDatabase);

    /**
     * 根據匹配記錄 ID 查詢詳情
     *
     * @param matchId                  匹配記錄 ID
     * @param studentProfilesDatabase  學生檔案庫名稱
     * @return 學生匹配詳情
     */
    SysStudentMatchVO selectStudentMatchVOById(
            @Param("matchId") Long matchId,
            @Param("studentProfilesDatabase") String studentProfilesDatabase);

    /**
     * 統計指定匹配記錄是否存在
     *
     * @param matchId 匹配記錄 ID
     * @return 記錄數
     */
    int countMatchById(@Param("matchId") Long matchId);

    /**
     * 根據學生檔案編號查詢匹配記錄 ID
     *
     * @param sysStudentMatchProfileNumDTO 檔案編號查詢條件
     * @return 匹配記錄 ID，不存在則為 null
     */
    Long selectMatchIdByProfileNum(
            @Param("sysStudentMatchProfileNumDTO") SysStudentMatchProfileNumDTO sysStudentMatchProfileNumDTO);

    /**
     * 新增匹配記錄（初始為未匹配、未同步）
     *
     * @param studentMatchInsertDTO 新增參數
     * @return 影響行數
     */
    int insertMatchRecord(
            @Param("studentMatchInsertDTO") SysStudentMatchInsertDTO studentMatchInsertDTO);

    /**
     * 更新學生匹配記錄
     *
     * @param studentMatchUpdateDTO 更新參數
     * @return 影響行數
     */
    int updateSysStudentMatch(
            @Param("studentMatchUpdateDTO") SysStudentMatchUpdateDTO studentMatchUpdateDTO);

    /**
     * 查詢未匹配學生列表
     *
     * @param studentMatchDTO          查詢條件
     * @param studentProfilesDatabase  學生檔案庫名稱
     * @return 未匹配學生列表
     */
    List<SysStudentMatchVO> selectUnmatchedList(
            @Param("studentMatchDTO") SysStudentMatchDTO studentMatchDTO,
            @Param("studentProfilesDatabase") String studentProfilesDatabase);

    /**
     * 查詢企微學生候選列表（尚未被匹配的企微學生）
     *
     * @param wecomStudentDTO 查詢條件
     * @return 企微學生候選列表
     */
    List<SysWecomStudentVO> selectWecomCandidates(
            @Param("wecomStudentDTO") SysWecomStudentDTO wecomStudentDTO);

    /**
     * 查詢全部企微學生信息（用於自動同步比對）
     *
     * @return 企微學生列表
     */
    List<SysWecomStudentVO> selectWecomStudentInfoList();

    /**
     * 更新企微學生姓名
     *
     * @param sysWecomStudentNameUpdateDTO 更新參數
     * @return 影響行數
     */
    int updateWecomStudentName(
            @Param("sysWecomStudentNameUpdateDTO") SysWecomStudentNameUpdateDTO sysWecomStudentNameUpdateDTO);

    /**
     * 批量刪除學生匹配記錄
     *
     * @param studentMatchDeleteDTO 刪除參數（含 matchIds）
     * @return 影響行數
     */
    int deleteSysStudentMatchByIds(
            @Param("studentMatchDeleteDTO") SysStudentMatchDeleteDTO studentMatchDeleteDTO);

    /**
     * 清除學生匹配關係（重置企微綁定與狀態，不刪除記錄）
     *
     * @param matchId 匹配記錄 ID
     * @return 影響行數
     */
    int clearStudentMatch(@Param("matchId") Long matchId);

    /**
     * 查詢學生與部門的綁定關係
     *
     * @param sysStudentMatchDeptQueryDTO 查詢條件（含 studentUserIds）
     * @return 學生部門綁定列表
     */
    List<SysStudentMatchDeptItemVO> selectStudentDeptBindings(
            @Param("sysStudentMatchDeptQueryDTO") SysStudentMatchDeptQueryDTO sysStudentMatchDeptQueryDTO);
}
