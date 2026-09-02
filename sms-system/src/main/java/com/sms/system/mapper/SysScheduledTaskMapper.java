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

    List<SysScheduledTask> selectAllTasks();

    SysScheduledTask selectByTaskKey(@Param("taskKey") String taskKey);

    int updateEnabled(@Param("taskKey") String taskKey, @Param("enabled") String enabled);

    int updateCronExpression(@Param("taskKey") String taskKey, @Param("cronExpression") String cronExpression);

    String selectTaskKeyByTaskBean(@Param("taskBean") String taskBean);

    int tryAcquireLock(@Param("taskKey") String taskKey,
                       @Param("lockOwner") String lockOwner,
                       @Param("leaseSeconds") int leaseSeconds);

    int releaseLock(@Param("taskKey") String taskKey, @Param("lockOwner") String lockOwner);
}
