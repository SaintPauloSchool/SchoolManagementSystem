package com.sms.system.service;

import com.sms.system.entity.query.SysStudentMatchQuery;
import com.sms.system.entity.vo.SysStudentMatchVO;
import com.sms.system.entity.vo.SysWecomStudentVO;
import java.util.List;
import java.util.Map;

/**
 * 學生數據匹配 業務層介面
 */
public interface ISysStudentMatchService {

    /**
     * 查詢學生數據匹配列表（學籍資料來自 student_profiles.student_info）
     */
    List<SysStudentMatchVO> selectSysStudentMatchList(SysStudentMatchQuery query);

    /**
     * 查詢未匹配的學籍數據列表
     */
    List<SysStudentMatchVO> selectUnmatchedList(SysStudentMatchQuery query);

    /**
     * 獲取待手動匹配的企業微信學生候選名單
     */
    List<SysWecomStudentVO> selectWecomCandidates(String queryName, String queryMobile, String queryClass);

    /**
     * 手動綁定學生匹配關係
     */
    boolean bindStudent(Long matchId, String studentProfileNum, String studentUserIdWecom);

    /**
     * 根據 matchId 列表取出待同步記錄（已匹配且未同步成功）
     */
    List<SysStudentMatchVO> getPendingListForSync(List<Long> matchIds);

    /**
     * 查詢指定學生的企微班級部門 ID 映射
     */
    Map<String, List<Long>> getStudentDeptMap(List<String> studentUserIds);

    /**
     * 保存單筆同步結果至資料庫
     */
    void saveOneSyncResult(SysStudentMatchVO match, String syncStatus, String errorMsg, String operName);

    /**
     * 同步/匹配數據（學籍資料與企微學生自動比對）
     */
    String syncData(String operName);

    /**
     * 批量刪除學生匹配記錄
     */
    int deleteSysStudentMatchByIds(List<Long> ids);

    /**
     * 清除單個匹配狀態
     */
    boolean clearMatch(Long matchId, String studentProfileNum);
}
