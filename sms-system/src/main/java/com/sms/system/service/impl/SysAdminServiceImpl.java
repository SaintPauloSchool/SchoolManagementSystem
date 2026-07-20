package com.sms.system.service.impl;

import com.sms.common.exception.ServiceException;
import com.sms.common.utils.StringUtils;
import com.sms.common.utils.bean.BeanCopyUtils;
import com.sms.system.entity.SysAdmin;
import com.sms.system.entity.dto.SysAdminBatchSaveDTO;
import com.sms.system.entity.dto.SysAdminQueryDTO;
import com.sms.system.entity.dto.SysAdminUpdateDTO;
import com.sms.system.entity.vo.SysAdminBatchInsertResultVO;
import com.sms.system.entity.vo.SysAdminCurrentUserVO;
import com.sms.system.entity.vo.SysAdminVO;
import com.sms.system.mapper.SysAdminMapper;
import com.sms.system.service.ISysAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 系統管理員 Service 業務層處理
 */
@Service
public class SysAdminServiceImpl implements ISysAdminService {

    private static final String TYPE_SUPER = "0";
    private static final String TYPE_ADMIN = "1";
    private static final String STATUS_NORMAL = "0";
    private static final String STATUS_DISABLE = "1";

    @Autowired
    private SysAdminMapper sysAdminMapper;

    @Override
    public boolean isNotAdmin(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return true;
        }
        SysAdmin admin = sysAdminMapper.selectByUserId(userId);
        return admin == null || !STATUS_NORMAL.equals(admin.getStatus());
    }

    @Override
    public boolean isNotSuperAdmin(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return true;
        }
        SysAdmin admin = sysAdminMapper.selectByUserId(userId);
        return admin == null
                || !STATUS_NORMAL.equals(admin.getStatus())
                || !TYPE_SUPER.equals(admin.getType());
    }

    @Override
    public SysAdmin selectByUserId(String userId) {
        return sysAdminMapper.selectByUserId(userId);
    }

    @Override
    public SysAdminCurrentUserVO selectCurrentUserInfo(String userId) {
        SysAdminCurrentUserVO vo = new SysAdminCurrentUserVO();
        vo.setIsAdmin(!isNotAdmin(userId));
        vo.setIsSuperAdmin(!isNotSuperAdmin(userId));
        SysAdmin current = sysAdminMapper.selectByUserId(userId);
        if (current != null) {
            vo.setType(current.getType());
            vo.setAdminName(current.getAdminName());
        }
        return vo;
    }

    @Override
    public List<SysAdminVO> selectList(SysAdminQueryDTO queryDTO) {
        List<SysAdmin> list = sysAdminMapper.selectList(queryDTO);
        return BeanCopyUtils.copyPageList(list, SysAdminVO.class);
    }

    @Override
    public SysAdminVO selectById(Long id) {
        return BeanCopyUtils.copy(sysAdminMapper.selectById(id), SysAdminVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysAdminBatchInsertResultVO batchInsert(SysAdminBatchSaveDTO saveDTO) {
        SysAdminBatchInsertResultVO result = new SysAdminBatchInsertResultVO();
        if (saveDTO == null || saveDTO.getAdmins() == null || saveDTO.getAdmins().isEmpty()) {
            throw new ServiceException("請至少選擇一位成員");
        }

        String defaultType = normalizeType(saveDTO.getType());
        LocalDateTime now = LocalDateTime.now();
        int success = 0;
        List<String> skipped = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (SysAdminBatchSaveDTO.SysAdminItem item : saveDTO.getAdmins()) {
            if (item == null || StringUtils.isEmpty(item.getUserId())) {
                skipped.add("缺少 WeCom 用戶ID，已跳過");
                continue;
            }
            String userId = item.getUserId().trim();
            if (!seen.add(userId)) {
                skipped.add(displayName(item) + "（重複選擇）");
                continue;
            }
            SysAdmin existing = sysAdminMapper.selectByUserId(userId);
            if (existing != null) {
                skipped.add(displayName(item) + "（已是管理員）");
                continue;
            }

            SysAdmin admin = new SysAdmin();
            admin.setUserId(userId);
            admin.setAdminName(StringUtils.isNotEmpty(item.getAdminName()) ? item.getAdminName().trim() : userId);
            admin.setType(StringUtils.isNotEmpty(item.getType()) ? normalizeType(item.getType()) : defaultType);
            admin.setStatus(STATUS_NORMAL);
            admin.setRemark(saveDTO.getRemark());
            admin.setCreateTime(now);
            admin.setUpdateTime(now);
            success += sysAdminMapper.insert(admin);
        }

        result.setSuccessCount(success);
        result.setSkipped(skipped);
        result.setMessage("成功新增 " + success + " 位管理員"
                + (skipped.isEmpty() ? "" : "，跳過 " + skipped.size() + " 位"));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateAdmin(SysAdminUpdateDTO updateDTO, String operatorUserId) {
        if (updateDTO == null || updateDTO.getId() == null) {
            throw new ServiceException("參數錯誤");
        }
        SysAdmin existing = sysAdminMapper.selectById(updateDTO.getId());
        if (existing == null) {
            throw new ServiceException("管理員不存在");
        }

        String newType = updateDTO.getType() != null ? normalizeType(updateDTO.getType()) : existing.getType();
        String newStatus = updateDTO.getStatus() != null ? normalizeStatus(updateDTO.getStatus()) : existing.getStatus();

        // 保護：不能把自己從超管降級 / 停用，導致系統無超管
        boolean demotingSelfSuper = existing.getUserId().equals(operatorUserId)
                && TYPE_SUPER.equals(existing.getType())
                && (!TYPE_SUPER.equals(newType) || !STATUS_NORMAL.equals(newStatus));
        if (demotingSelfSuper) {
            throw new ServiceException("不能修改自己的超級管理員身份或停用自己的帳號");
        }

        // 保護：最後一位正常超管不可被降級/停用
        if (TYPE_SUPER.equals(existing.getType()) && STATUS_NORMAL.equals(existing.getStatus())
                && (!TYPE_SUPER.equals(newType) || !STATUS_NORMAL.equals(newStatus))) {
            int superCount = sysAdminMapper.countByTypeAndStatus(TYPE_SUPER, STATUS_NORMAL);
            if (superCount <= 1) {
                throw new ServiceException("系統至少需要保留一位正常狀態的超級管理員");
            }
        }

        SysAdmin toUpdate = new SysAdmin();
        toUpdate.setId(updateDTO.getId());
        if (updateDTO.getAdminName() != null) {
            toUpdate.setAdminName(updateDTO.getAdminName());
        }
        if (updateDTO.getType() != null) {
            toUpdate.setType(newType);
        }
        if (updateDTO.getStatus() != null) {
            toUpdate.setStatus(newStatus);
        }
        if (updateDTO.getRemark() != null) {
            toUpdate.setRemark(updateDTO.getRemark());
        }
        toUpdate.setUpdateTime(LocalDateTime.now());
        return sysAdminMapper.updateById(toUpdate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(Long[] ids, String operatorUserId) {
        if (ids == null || ids.length == 0) {
            throw new ServiceException("請選擇要刪除的管理員");
        }

        int deleted = 0;
        for (Long id : ids) {
            SysAdmin existing = sysAdminMapper.selectById(id);
            if (existing == null) {
                continue;
            }
            if (existing.getUserId().equals(operatorUserId)) {
                throw new ServiceException("不能刪除自己的管理員帳號");
            }
            if (TYPE_SUPER.equals(existing.getType()) && STATUS_NORMAL.equals(existing.getStatus())) {
                int superCount = sysAdminMapper.countByTypeAndStatus(TYPE_SUPER, STATUS_NORMAL);
                if (superCount <= 1) {
                    throw new ServiceException("不能刪除最後一位超級管理員");
                }
            }
            deleted += sysAdminMapper.deleteById(id);
        }
        return deleted;
    }

    private String normalizeType(String type) {
        if (TYPE_SUPER.equals(type)) {
            return TYPE_SUPER;
        }
        return TYPE_ADMIN;
    }

    private String normalizeStatus(String status) {
        if (STATUS_DISABLE.equals(status)) {
            return STATUS_DISABLE;
        }
        return STATUS_NORMAL;
    }

    private String displayName(SysAdminBatchSaveDTO.SysAdminItem item) {
        if (item == null) {
            return "未知";
        }
        if (StringUtils.isNotEmpty(item.getAdminName())) {
            return item.getAdminName();
        }
        return item.getUserId();
    }
}
