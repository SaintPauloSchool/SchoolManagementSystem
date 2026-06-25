package com.sms.system.service.impl;

import com.sms.system.entity.SysTaskLog;
import com.sms.system.entity.dto.SysTaskLogInsertDTO;
import com.sms.system.entity.dto.SysTaskLogQueryDTO;
import com.sms.system.entity.dto.SysTaskLogUpdateDTO;
import com.sms.system.entity.vo.SysTaskLogVO;
import com.sms.system.mapper.SysTaskLogMapper;
import com.sms.system.service.ISysTaskLogService;
import com.sms.common.utils.bean.BeanCopyUtils;
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

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertTaskLog(SysTaskLogInsertDTO sysTaskLogInsertDTO) {
        SysTaskLog sysTaskLog = BeanCopyUtils.copy(sysTaskLogInsertDTO, SysTaskLog.class);
        sysTaskLogMapper.insertTaskLog(sysTaskLog);
    }

    @Override
    public List<SysTaskLogVO> selectTaskLogList(SysTaskLogQueryDTO sysTaskLogQueryDTO) {
        SysTaskLog sysTaskLogQuery = BeanCopyUtils.copy(sysTaskLogQueryDTO, SysTaskLog.class);
        List<SysTaskLog> sysTaskLogList = sysTaskLogMapper.selectTaskLogList(sysTaskLogQuery);
        return BeanCopyUtils.copyPageList(sysTaskLogList, SysTaskLogVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTaskLog(SysTaskLogUpdateDTO sysTaskLogUpdateDTO) {
        SysTaskLog sysTaskLog = BeanCopyUtils.copy(sysTaskLogUpdateDTO, SysTaskLog.class);
        return sysTaskLogMapper.updateTaskLog(sysTaskLog);
    }

    @Override
    public int countFailedUnprocessedTasks() {
        return sysTaskLogMapper.countFailedUnprocessedTasks();
    }
}
