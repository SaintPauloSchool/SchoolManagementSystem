package com.sms.system.mapper;

import com.sms.system.entity.SysTaskLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 定時任務執行日誌 Mapper 介面
 */
@Mapper
public interface SysTaskLogMapper {
    /**
     * 新增任務日誌
     *
     * @param sysTaskLog 任務日誌物件
     * @return 影響行數
     */
    int insertTaskLog(SysTaskLog sysTaskLog);

    /**
     * 查詢任務日誌列表
     *
     * @param sysTaskLog 任務日誌(帶查詢條件)
     * @return 任務日誌集合
     */
    List<SysTaskLog> selectTaskLogList(SysTaskLog sysTaskLog);
}
