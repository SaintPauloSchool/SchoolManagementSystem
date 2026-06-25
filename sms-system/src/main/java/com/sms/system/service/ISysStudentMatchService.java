package com.sms.system.service;

import com.sms.system.entity.dto.SysStudentMatchBindDTO;
import com.sms.system.entity.dto.SysStudentMatchDTO;
import com.sms.system.entity.dto.SysStudentMatchDeleteDTO;
import com.sms.system.entity.dto.SysStudentMatchDeptQueryDTO;
import com.sms.system.entity.dto.SysStudentMatchSyncDataDTO;
import com.sms.system.entity.dto.SysStudentMatchSyncDTO;
import com.sms.system.entity.dto.SysStudentMatchSyncRecordDTO;
import com.sms.common.core.page.TableDataInfo;
import com.sms.system.entity.dto.SysWecomStudentDTO;
import com.sms.system.entity.vo.SysStudentMatchDeptMapVO;
import com.sms.system.entity.vo.SysStudentMatchOperationResultVO;
import com.sms.system.entity.vo.SysStudentMatchVO;

import java.util.List;

/**
 * 學生數據匹配 業務層介面
 * <p>入參：DTO；出參：VO</p>
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
     * 查詢企微學生候選列表（業務層篩選與分頁）
     */
    TableDataInfo selectWecomCandidates(SysWecomStudentDTO wecomStudentDTO);

    /**
     * 手動綁定學生匹配
     */
    SysStudentMatchOperationResultVO bindStudent(SysStudentMatchBindDTO studentMatchBindDTO);

    /**
     * 查詢待同步的匹配記錄列表
     */
    List<SysStudentMatchVO> getPendingListForSync(SysStudentMatchSyncDTO studentMatchSyncDTO);

    /**
     * 查詢學生所屬部門映射
     */
    SysStudentMatchDeptMapVO getStudentDeptMap(SysStudentMatchDeptQueryDTO sysStudentMatchDeptQueryDTO);

    /**
     * 保存單條同步結果
     */
    void saveOneSyncResult(SysStudentMatchSyncRecordDTO syncRecordDTO);

    /**
     * 同步對照數據（自動比對綁定）
     */
    SysStudentMatchOperationResultVO syncData(SysStudentMatchSyncDataDTO syncDataDTO);

    /**
     * 批量刪除學生匹配記錄
     */
    SysStudentMatchOperationResultVO deleteSysStudentMatchByIds(SysStudentMatchDeleteDTO studentMatchDeleteDTO);
}
