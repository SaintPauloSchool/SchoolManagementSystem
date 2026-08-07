package com.sms.handler.student;

import com.sms.system.entity.dto.SysStudentMatchSyncDataDTO;
import com.sms.system.entity.task.TaskResult;
import com.sms.system.entity.vo.SysStudentMatchOperationResultVO;
import com.sms.system.service.ISysStudentMatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 學生數據自動匹配處理器（定時任務調用）
 */
@Component
public class StudentMatchSyncHandler {

    private static final Logger log = LoggerFactory.getLogger(StudentMatchSyncHandler.class);

    @Autowired
    private ISysStudentMatchService sysStudentMatchService;

    /**
     * 按班級 + 姓名自動比對學籍與本地企微家校通訊錄，寫入匹配記錄。
     */
    public TaskResult syncStudentMatch() {
        log.info("開始執行學生數據自動匹配");
        SysStudentMatchSyncDataDTO syncDataDTO = new SysStudentMatchSyncDataDTO();
        syncDataDTO.setOperName("system");
        SysStudentMatchOperationResultVO result = sysStudentMatchService.syncData(syncDataDTO);
        int affectedCount = result.getAffectedCount() != null ? result.getAffectedCount() : 0;
        if (result.isSuccess()) {
            log.info("學生數據自動匹配完成：{}", result.getMessage());
            return TaskResult.success(affectedCount, 0, result.getMessage());
        }
        log.warn("學生數據自動匹配失敗：{}", result.getMessage());
        return TaskResult.fail(0, 1, result.getMessage());
    }
}
