package com.sms.system.mapper;

import com.sms.system.entity.SysUserRole;
import com.sms.system.entity.dto.SysUserRoleQueryDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系統用戶角色 Mapper
 */
public interface SysUserRoleMapper {

    List<String> selectActiveUserIds();

    SysUserRole selectByUserId(@Param("userId") String userId);

    SysUserRole selectById(@Param("id") Long id);

    List<SysUserRole> selectList(SysUserRoleQueryDTO queryDTO);

    int countByTypeAndStatus(@Param("type") String type, @Param("status") String status);

    int insert(SysUserRole userRole);

    int updateById(SysUserRole userRole);

    int deleteById(@Param("id") Long id);

    int deleteByIds(@Param("ids") Long[] ids);
}
