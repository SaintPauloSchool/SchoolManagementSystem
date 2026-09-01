package com.sms.system.service;

import com.sms.system.entity.SysUserRole;
import com.sms.system.entity.dto.SysUserRoleBatchSaveDTO;
import com.sms.system.entity.dto.SysUserRoleQueryDTO;
import com.sms.system.entity.dto.SysUserRoleUpdateDTO;
import com.sms.system.entity.vo.SysUserRoleBatchInsertResultVO;
import com.sms.system.entity.vo.SysUserRoleCurrentUserVO;
import com.sms.system.entity.vo.SysUserRoleVO;

import java.util.List;

/**
 * 系統用戶角色 Service
 */
public interface ISysUserRoleService {

    boolean isNotUserRole(String userId);

    boolean isNotSuperUserRole(String userId);

    /** 是否為管理員（type 0/1，不含 type 2） */
    boolean hasAdminUserRole(String userId);

    SysUserRole selectByUserId(String userId);

    SysUserRoleCurrentUserVO selectCurrentUserInfo(String userId, String loginName);

    String resolveSenderDisplayName(String userId, String loginName);

    List<SysUserRoleVO> selectList(SysUserRoleQueryDTO queryDTO);

    SysUserRoleVO selectById(Long id);

    SysUserRoleBatchInsertResultVO batchInsert(SysUserRoleBatchSaveDTO saveDTO);

    int updateUserRole(SysUserRoleUpdateDTO updateDTO, String operatorUserId);

    int deleteByIds(Long[] ids, String operatorUserId);
}
