package com.sms.system.service.impl;

import com.sms.system.entity.SysDepartmentAdmin;
import com.sms.system.mapper.SysDepartmentAdminMapper;
import com.sms.system.service.ISysDepartmentAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SysDepartmentAdminServiceImpl implements ISysDepartmentAdminService {

    private static final Logger logger = LoggerFactory.getLogger(SysDepartmentAdminServiceImpl.class);

    @Autowired
    private SysDepartmentAdminMapper departmentAdminMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncDepartmentAdminsFromWecom(List<SysDepartmentAdmin> admins,
                                              Collection<Long> syncedDepartmentIds) {
        if (syncedDepartmentIds == null || syncedDepartmentIds.isEmpty()) {
            return;
        }

        Set<Long> syncedDeptIdSet = syncedDepartmentIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (syncedDeptIdSet.isEmpty()) {
            return;
        }

        // 企微最新名單：key = departmentId|userid
        Map<String, SysDepartmentAdmin> wecomMap = new HashMap<>();
        if (admins != null) {
            for (SysDepartmentAdmin admin : admins) {
                if (admin == null
                        || admin.getDepartmentId() == null
                        || !StringUtils.hasText(admin.getUserid())) {
                    continue;
                }
                wecomMap.put(buildKey(admin.getDepartmentId(), admin.getUserid()), admin);
            }
        }

        List<SysDepartmentAdmin> existingList = departmentAdminMapper.selectAll();
        if (existingList == null) {
            existingList = new ArrayList<>();
        }

        List<SysDepartmentAdmin> toInsert = new ArrayList<>();
        List<SysDepartmentAdmin> toUpdate = new ArrayList<>();
        List<Long> toDeleteIds = new ArrayList<>();
        Set<String> existingKeys = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();

        for (SysDepartmentAdmin existing : existingList) {
            if (existing == null || existing.getId() == null || existing.getDepartmentId() == null) {
                continue;
            }
            // 部門已不在企微列表：整筆刪除（舊學段殘留）
            if (!syncedDeptIdSet.contains(existing.getDepartmentId())) {
                toDeleteIds.add(existing.getId());
                continue;
            }
            if (!StringUtils.hasText(existing.getUserid())) {
                toDeleteIds.add(existing.getId());
                continue;
            }

            String key = buildKey(existing.getDepartmentId(), existing.getUserid());
            existingKeys.add(key);
            SysDepartmentAdmin wecomAdmin = wecomMap.get(key);
            if (wecomAdmin == null) {
                // 該部門下此人已不在企微管理員名單
                toDeleteIds.add(existing.getId());
                continue;
            }
            if (needsUpdate(existing, wecomAdmin)) {
                existing.setType(wecomAdmin.getType());
                existing.setSubject(wecomAdmin.getSubject());
                existing.setUpdateTime(now);
                toUpdate.add(existing);
            }
        }

        for (Map.Entry<String, SysDepartmentAdmin> entry : wecomMap.entrySet()) {
            if (existingKeys.contains(entry.getKey())) {
                continue;
            }
            SysDepartmentAdmin wecomAdmin = entry.getValue();
            wecomAdmin.setCreateTime(now);
            wecomAdmin.setUpdateTime(now);
            toInsert.add(wecomAdmin);
        }

        if (!toDeleteIds.isEmpty()) {
            int batchSize = 500;
            for (int i = 0; i < toDeleteIds.size(); i += batchSize) {
                List<Long> batch = toDeleteIds.subList(i, Math.min(i + batchSize, toDeleteIds.size()));
                departmentAdminMapper.deleteBatchByIds(batch);
            }
            logger.info("刪除過期部門管理員 {} 條", toDeleteIds.size());
        }

        if (!toInsert.isEmpty()) {
            int batchSize = 500;
            for (int i = 0; i < toInsert.size(); i += batchSize) {
                List<SysDepartmentAdmin> batch = toInsert.subList(i, Math.min(i + batchSize, toInsert.size()));
                departmentAdminMapper.batchInsertDepartmentAdmins(batch);
            }
            logger.info("新增部門管理員 {} 條", toInsert.size());
        }

        if (!toUpdate.isEmpty()) {
            for (SysDepartmentAdmin admin : toUpdate) {
                departmentAdminMapper.updateByDepartmentIdAndUserid(admin);
            }
            logger.info("更新部門管理員 {} 條", toUpdate.size());
        }

        logger.info("部門管理員差量同步完成 - 新增: {}, 更新: {}, 刪除: {}, 企微總數: {}",
                toInsert.size(), toUpdate.size(), toDeleteIds.size(), wecomMap.size());
    }

    private String buildKey(Long departmentId, String userid) {
        return departmentId + "|" + userid.trim();
    }

    private boolean needsUpdate(SysDepartmentAdmin existing, SysDepartmentAdmin wecom) {
        return !Objects.equals(existing.getType(), wecom.getType())
                || !Objects.equals(normalize(existing.getSubject()), normalize(wecom.getSubject()));
    }

    private String normalize(String value) {
        return value == null ? null : value;
    }
}
