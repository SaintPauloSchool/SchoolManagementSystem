package com.sms.system.mapper;

import com.sms.system.entity.SysScheduledTask;
import com.sms.system.entity.vo.SysScheduledTaskVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 定時任務配置 Mapper
 */
public interface SysScheduledTaskMapper {

    List<SysScheduledTaskVO> selectTaskList();

    SysScheduledTask selectByTaskKey(@Param("taskKey") String taskKey);

    int updateEnabled(@Param("taskKey") String taskKey, @Param("enabled") String enabled);
}
