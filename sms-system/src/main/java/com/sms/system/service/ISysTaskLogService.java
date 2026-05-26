package com.sms.system.service;

import com.sms.system.entity.SysTaskLog;
import java.util.List;

/**
 * 定時任務執行日誌 服務層
 */
public interface ISysTaskLogService {
    /**
     * 新增任務日誌
     *
     * @param sysTaskLog 任務日誌
     */
    void insertTaskLog(SysTaskLog sysTaskLog);

    /**
     * 查詢任務日誌列表
     *
     * @param sysTaskLog 任務日誌(帶查詢條件)
     * @return 任務日誌集合
     */
    List<SysTaskLog> selectTaskLogList(SysTaskLog sysTaskLog);

    /**
     * 修改任務日誌
     *
     * @param sysTaskLog 任務日誌
     * @return 影響行數
     */
    int updateTaskLog(SysTaskLog sysTaskLog);

    /**
     * 統計失敗且未處理的任務數量
     *
     * @return 數量
     */
    int countFailedUnprocessedTasks();
}
