package com.sms.system.service.impl;

import com.sms.common.exception.ServiceException;
import com.sms.common.utils.StringUtils;
import com.sms.common.utils.bean.BeanCopyUtils;
import com.sms.system.entity.SysUserRole;
import com.sms.system.entity.dto.SysUserRoleBatchSaveDTO;
import com.sms.system.entity.dto.SysUserRoleQueryDTO;
import com.sms.system.entity.dto.SysUserRoleUpdateDTO;
import com.sms.system.entity.vo.SysUserRoleBatchInsertResultVO;
import com.sms.system.entity.vo.SysUserRoleCurrentUserVO;
import com.sms.system.entity.vo.SysUserRoleVO;
import com.sms.system.mapper.SysUserRoleMapper;
import com.sms.system.service.ISysUserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 系統用戶角色 Service
 */
@Service
public class SysUserRoleServiceImpl implements ISysUserRoleService {

    private static final String TYPE_SUPER = "0";
    private static final String TYPE_ADMIN = "1";
    private static final String TYPE_OTHER = "2";
    private static final String STATUS_NORMAL = "0";
    private static final String STATUS_DISABLE = "1";

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Override
    public boolean isNotUserRole(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return true;
        }
        SysUserRole userRole = sysUserRoleMapper.selectByUserId(userId);
        return userRole == null || !STATUS_NORMAL.equals(userRole.getStatus());
    }

    @Override
    public boolean isNotSuperUserRole(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return true;
        }
        SysUserRole userRole = sysUserRoleMapper.selectByUserId(userId);
        return userRole == null
                || !STATUS_NORMAL.equals(userRole.getStatus())
                || !TYPE_SUPER.equals(userRole.getType());
    }

    @Override
    public SysUserRole selectByUserId(String userId) {
        return sysUserRoleMapper.selectByUserId(userId);
    }

    @Override
    public SysUserRoleCurrentUserVO selectCurrentUserInfo(String userId, String loginName) {
        SysUserRoleCurrentUserVO vo = new SysUserRoleCurrentUserVO();
        vo.setHasUserRole(!isNotUserRole(userId));
        vo.setHasSuperUserRole(!isNotSuperUserRole(userId));
        SysUserRole current = sysUserRoleMapper.selectByUserId(userId);
        if (current != null) {
            vo.setType(current.getType());
            vo.setUserName(current.getUserName());
            vo.setSenderDisplayName(current.getSenderDisplayName());
        }
        vo.setSenderName(resolveSenderDisplayName(userId, loginName));
        return vo;
    }

    @Override
    public String resolveSenderDisplayName(String userId, String loginName) {
        if (StringUtils.isEmpty(userId)) {
            return StringUtils.isNotEmpty(loginName) ? loginName : "";
        }
        SysUserRole userRole = sysUserRoleMapper.selectByUserId(userId);
        if (userRole != null
                && STATUS_NORMAL.equals(userRole.getStatus())
                && StringUtils.isNotEmpty(userRole.getSenderDisplayName())) {
            return userRole.getSenderDisplayName().trim();
        }
        return StringUtils.isNotEmpty(loginName) ? loginName : "";
    }

    @Override
    public List<SysUserRoleVO> selectList(SysUserRoleQueryDTO queryDTO) {
        List<SysUserRole> list = sysUserRoleMapper.selectList(queryDTO);
        return BeanCopyUtils.copyPageList(list, SysUserRoleVO.class);
    }

    @Override
    public SysUserRoleVO selectById(Long id) {
        return BeanCopyUtils.copy(sysUserRoleMapper.selectById(id), SysUserRoleVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUserRoleBatchInsertResultVO batchInsert(SysUserRoleBatchSaveDTO saveDTO) {
        SysUserRoleBatchInsertResultVO result = new SysUserRoleBatchInsertResultVO();
        if (saveDTO == null || saveDTO.getUserRoles() == null || saveDTO.getUserRoles().isEmpty()) {
            throw new ServiceException("請至少選擇一位成員");
        }

        String defaultType = normalizeType(saveDTO.getType());
        LocalDateTime now = LocalDateTime.now();
        int success = 0;
        List<String> skipped = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (SysUserRoleBatchSaveDTO.SysUserRoleItem item : saveDTO.getUserRoles()) {
            if (item == null || StringUtils.isEmpty(item.getUserId())) {
                skipped.add("缺少 WeCom 用戶ID，已跳過");
                continue;
            }
            String userId = item.getUserId().trim();
            if (!seen.add(userId)) {
                skipped.add(displayName(item) + "（重複選擇）");
                continue;
            }
            SysUserRole existing = sysUserRoleMapper.selectByUserId(userId);
            if (existing != null) {
                skipped.add(displayName(item) + "（已存在）");
                continue;
            }

            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setUserName(StringUtils.isNotEmpty(item.getUserName()) ? item.getUserName().trim() : userId);
            userRole.setType(StringUtils.isNotEmpty(item.getType()) ? normalizeType(item.getType()) : defaultType);
            userRole.setSenderDisplayName(resolveSenderDisplayNameForSave(item.getSenderDisplayName(), saveDTO.getSenderDisplayName()));
            userRole.setStatus(STATUS_NORMAL);
            userRole.setRemark(saveDTO.getRemark());
            userRole.setCreateTime(now);
            userRole.setUpdateTime(now);
            success += sysUserRoleMapper.insert(userRole);
        }

        result.setSuccessCount(success);
        result.setSkipped(skipped);
        result.setMessage("成功新增 " + success + " 位用戶角色"
                + (skipped.isEmpty() ? "" : "，跳過 " + skipped.size() + " 位"));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateUserRole(SysUserRoleUpdateDTO updateDTO, String operatorUserId) {
        if (updateDTO == null || updateDTO.getId() == null) {
            throw new ServiceException("參數錯誤");
        }
        SysUserRole existing = sysUserRoleMapper.selectById(updateDTO.getId());
        if (existing == null) {
            throw new ServiceException("用戶角色不存在");
        }

        String newType = updateDTO.getType() != null ? normalizeType(updateDTO.getType()) : existing.getType();
        String newStatus = updateDTO.getStatus() != null ? normalizeStatus(updateDTO.getStatus()) : existing.getStatus();

        boolean demotingSelfSuper = existing.getUserId().equals(operatorUserId)
                && TYPE_SUPER.equals(existing.getType())
                && (!TYPE_SUPER.equals(newType) || !STATUS_NORMAL.equals(newStatus));
        if (demotingSelfSuper) {
            throw new ServiceException("不能修改自己的超級管理員身份或停用自己的帳號");
        }

        if (TYPE_SUPER.equals(existing.getType()) && STATUS_NORMAL.equals(existing.getStatus())
                && (!TYPE_SUPER.equals(newType) || !STATUS_NORMAL.equals(newStatus))) {
            int superCount = sysUserRoleMapper.countByTypeAndStatus(TYPE_SUPER, STATUS_NORMAL);
            if (superCount <= 1) {
                throw new ServiceException("系統至少需要保留一位正常狀態的超級管理員");
            }
        }

        SysUserRole toUpdate = new SysUserRole();
        toUpdate.setId(updateDTO.getId());
        if (updateDTO.getUserName() != null) {
            toUpdate.setUserName(updateDTO.getUserName());
        }
        if (updateDTO.getSenderDisplayName() != null) {
            toUpdate.setSenderDisplayName(StringUtils.isNotEmpty(updateDTO.getSenderDisplayName())
                    ? updateDTO.getSenderDisplayName().trim()
                    : "");
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
        return sysUserRoleMapper.updateById(toUpdate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(Long[] ids, String operatorUserId) {
        if (ids == null || ids.length == 0) {
            throw new ServiceException("請選擇要刪除的用戶角色");
        }

        int deleted = 0;
        for (Long id : ids) {
            SysUserRole existing = sysUserRoleMapper.selectById(id);
            if (existing == null) {
                continue;
            }
            if (existing.getUserId().equals(operatorUserId)) {
                throw new ServiceException("不能刪除自己的帳號");
            }
            if (TYPE_SUPER.equals(existing.getType()) && STATUS_NORMAL.equals(existing.getStatus())) {
                int superCount = sysUserRoleMapper.countByTypeAndStatus(TYPE_SUPER, STATUS_NORMAL);
                if (superCount <= 1) {
                    throw new ServiceException("不能刪除最後一位超級管理員");
                }
            }
            deleted += sysUserRoleMapper.deleteById(id);
        }
        return deleted;
    }

    private String normalizeType(String type) {
        if (TYPE_SUPER.equals(type)) {
            return TYPE_SUPER;
        }
        if (TYPE_OTHER.equals(type)) {
            return TYPE_OTHER;
        }
        return TYPE_ADMIN;
    }

    private String normalizeStatus(String status) {
        if (STATUS_DISABLE.equals(status)) {
            return STATUS_DISABLE;
        }
        return STATUS_NORMAL;
    }

    private String resolveSenderDisplayNameForSave(String itemValue, String defaultValue) {
        if (StringUtils.isNotEmpty(itemValue)) {
            return itemValue.trim();
        }
        if (StringUtils.isNotEmpty(defaultValue)) {
            return defaultValue.trim();
        }
        return null;
    }

    private String displayName(SysUserRoleBatchSaveDTO.SysUserRoleItem item) {
        if (item == null) {
            return "未知";
        }
        if (StringUtils.isNotEmpty(item.getUserName())) {
            return item.getUserName();
        }
        return item.getUserId();
    }
}
