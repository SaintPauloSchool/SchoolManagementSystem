package com.sms.system.service;

import com.sms.system.entity.dto.SysStudentMatchBindDTO;
import com.sms.system.entity.dto.SysStudentMatchDTO;
import com.sms.system.entity.dto.SysStudentMatchSyncDataDTO;
import com.sms.system.entity.dto.SysWecomStudentDTO;
import com.sms.system.entity.vo.SysSchoolFamilyContactVO;
import com.sms.system.entity.vo.SysStudentMatchOperationResultVO;
import com.sms.system.entity.vo.SysStudentMatchVO;

import java.util.List;

/**
 * 學生數據匹配
 */
public interface ISysStudentMatchService {

    /**
     * 查詢學生匹配列表
     */
    List<SysStudentMatchVO> selectSysStudentMatchList(SysStudentMatchDTO studentMatchDTO);

    /**
     * 查詢未匹配學生列表
     */
    List<SysStudentMatchVO> selectUnmatchedList(SysStudentMatchDTO studentMatchDTO);

    /**
     * 查詢企微學生候選列表（篩選條件在 SQL，分頁由 Controller startPage 觸發）
     */
    List<SysSchoolFamilyContactVO> selectWecomCandidates(SysWecomStudentDTO wecomStudentDTO);

    /**
     * 手動綁定學生匹配
     *
     * @return 綁定是否成功
     */
    boolean bindStudent(SysStudentMatchBindDTO studentMatchBindDTO);

    /**
     * 同步對照數據（自動比對綁定）
     */
    SysStudentMatchOperationResultVO syncData(SysStudentMatchSyncDataDTO syncDataDTO);
}
