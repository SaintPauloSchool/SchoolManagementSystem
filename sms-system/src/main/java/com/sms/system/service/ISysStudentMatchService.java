package com.sms.system.service;

import com.sms.system.entity.SysStudentMatch;
import com.sms.system.entity.vo.SysWecomStudentVO;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 學生數據匹配 業務層介面
 */
public interface ISysStudentMatchService {

    /**
     * 下載學籍導入模板
     */
    void downloadTemplate(HttpServletResponse response) throws IOException;


    /** 查詢學生數據匹配列表 */
    List<SysStudentMatch> selectSysStudentMatchList(SysStudentMatch sysStudentMatch);



    /** 導入 Excel 學籍數據 */
    String importExcel(MultipartFile file, String operName) throws Exception;

    /** 獲取未匹配的本地學籍導入列表 */
    List<SysStudentMatch> selectUnmatchedList(SysStudentMatch sysStudentMatch);

    /** 獲取待手動匹配的企業微信學生候選名單 */
    List<SysWecomStudentVO> selectWecomCandidates(String queryName, String queryMobile, String queryClass);

    /** 手動綁定學生關係 */
    boolean bindStudent(Long matchId, String studentUserIdWecom);

    /**
     * 根據 matchId 列表取出待同步記錄（已匹配且未同步成功）
     * 供 sms-api StudentMatchHandler 使用
     */
    List<SysStudentMatch> getPendingListForSync(List<Long> matchIds);

    /**
     * 查詢指定學生的企微班級部門 ID 映射
     * Key: studentUserIdWecom, Value: 部門 ID 列表
     * 供 sms-api StudentMatchHandler 使用
     */
    Map<String, List<Long>> getStudentDeptMap(List<String> studentUserIds);

    /**
     * 保存單筆同步結果至資料庫
     * 成功時同步更新本地 sys_parent_student_relation.student_name
     * 供 sms-api StudentMatchHandler 使用
     */
    void saveOneSyncResult(SysStudentMatch match, String syncStatus, String errorMsg, String operName);

    /** 同步/匹配數據 (本地數據比對匹配) */
    String syncData(String operName);

    /** 批量刪除學生數據匹配 */
    int deleteSysStudentMatchByIds(List<Long> ids);

    /** 清除單個匹配狀態 */
    boolean clearMatch(Long matchId);

    /**
     * 清空所有對照數據
     */
    void deleteAllSysStudentMatch();
}
