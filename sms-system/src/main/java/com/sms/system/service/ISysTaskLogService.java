package com.sms.system.service;

import com.sms.system.entity.dto.SysTaskLogInsertDTO;
import com.sms.system.entity.dto.SysTaskLogQueryDTO;
import com.sms.system.entity.dto.SysTaskLogUpdateDTO;
import com.sms.system.entity.vo.SysTaskLogVO;

import java.util.List;

/**
 * 定時任務執行日誌 服務層
 */
public interface ISysTaskLogService {
    /**
     * 新增任務日誌
     *
     * @param sysTaskLogInsertDTO 任務日誌
     */
    void insertTaskLog(SysTaskLogInsertDTO sysTaskLogInsertDTO);

    /**
     * 查詢任務日誌列表
     *
     * @param sysTaskLogQueryDTO 查詢條件
     * @return 任務日誌集合
     */
    List<SysTaskLogVO> selectTaskLogList(SysTaskLogQueryDTO sysTaskLogQueryDTO);

    /**
     * 修改任務日誌
     *
     * @param sysTaskLogUpdateDTO 任務日誌
     * @return 影響行數
     */
    int updateTaskLog(SysTaskLogUpdateDTO sysTaskLogUpdateDTO);

    /**
     * 統計失敗且未處理的任務數量
     *
     * @return 數量
     */
    int countFailedUnprocessedTasks();
}
