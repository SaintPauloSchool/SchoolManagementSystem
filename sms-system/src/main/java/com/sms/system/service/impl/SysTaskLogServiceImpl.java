package com.sms.system.service.impl;

import com.sms.system.entity.SysTaskLog;
import com.sms.system.mapper.SysTaskLogMapper;
import com.sms.system.service.ISysTaskLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 定時任務執行日誌 服務層實現
 */
@Service
public class SysTaskLogServiceImpl implements ISysTaskLogService {

    @Autowired
    private SysTaskLogMapper sysTaskLogMapper;

    /**
     * 新增任務日誌
     * 使用 REQUIRES_NEW 確保即使外層事務回滾，日誌依然能夠被提交。
     *
     * @param sysTaskLog 任務日誌
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertTaskLog(SysTaskLog sysTaskLog) {
        sysTaskLogMapper.insertTaskLog(sysTaskLog);
    }

    /**
     * 查詢任務日誌列表
     *
     * @param sysTaskLog 任務日誌
     * @return 任務日誌集合
     */
    @Override
    public List<SysTaskLog> selectTaskLogList(SysTaskLog sysTaskLog) {
        return sysTaskLogMapper.selectTaskLogList(sysTaskLog);
    }

    /**
     * 修改任務日誌
     *
     * @param sysTaskLog 任務日誌
     * @return 影響行數
     */
    @Override
    public int updateTaskLog(SysTaskLog sysTaskLog) {
        return sysTaskLogMapper.updateTaskLog(sysTaskLog);
    }

    /**
     * 統計失敗且未處理的任務數量
     *
     * @return 數量
     */
    @Override
    public int countFailedUnprocessedTasks() {
        return sysTaskLogMapper.countFailedUnprocessedTasks();
    }
}
