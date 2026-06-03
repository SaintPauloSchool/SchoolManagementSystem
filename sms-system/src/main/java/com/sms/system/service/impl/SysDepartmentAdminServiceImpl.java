package com.sms.system.service.impl;

import com.sms.system.entity.SysDepartmentAdmin;
import com.sms.system.mapper.SysDepartmentAdminMapper;
import com.sms.system.service.ISysDepartmentAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SysDepartmentAdminServiceImpl implements ISysDepartmentAdminService {

    private static final Logger logger = LoggerFactory.getLogger(SysDepartmentAdminServiceImpl.class);

    @Autowired
    private SysDepartmentAdminMapper departmentAdminMapper;

    @Override
    @Transactional
    public void batchSaveDepartmentAdmins(List<SysDepartmentAdmin> admins) {
        if (admins == null || admins.isEmpty()) {
            return;
        }

        List<SysDepartmentAdmin> toInsert = new ArrayList<>();
        List<SysDepartmentAdmin> toUpdate = new ArrayList<>();

        for (SysDepartmentAdmin admin : admins) {
            SysDepartmentAdmin existing = departmentAdminMapper.selectByDepartmentIdAndUserid(
                    admin.getDepartmentId(), admin.getUserid());

            if (existing != null) {
                admin.setId(existing.getId());
                admin.setUpdateTime(LocalDateTime.now());
                toUpdate.add(admin);
            } else {
                admin.setCreateTime(LocalDateTime.now());
                admin.setUpdateTime(LocalDateTime.now());
                toInsert.add(admin);
            }
        }

        if (!toInsert.isEmpty()) {
            departmentAdminMapper.batchInsertDepartmentAdmins(toInsert);
            logger.info("新增部門管理員 {} 條", toInsert.size());
        }

        if (!toUpdate.isEmpty()) {
            for (SysDepartmentAdmin admin : toUpdate) {
                departmentAdminMapper.updateByDepartmentIdAndUserid(admin);
            }
            logger.info("更新部門管理員 {} 條", toUpdate.size());
        }

        logger.info("部門管理員同步完成 - 新增: {}, 更新: {}", toInsert.size(), toUpdate.size());
    }
}
