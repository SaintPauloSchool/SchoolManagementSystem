package com.sms.system.mapper;

import com.sms.system.entity.SysAdmin;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统管理员 Mapper 接口
 */
public interface SysAdminMapper {
    /**
     * 获取所有状态正常的管理员用户ID列表
     *
     * @return 用户ID列表
     */
    List<String> selectAdminUserIds();
    /**
     * 根据用户ID查询管理员信息
     *
     * @param userId 用户ID
     * @return 管理员信息
     */
    SysAdmin selectByUserId(@Param("userId") String userId);

    /**
     * 新增管理员
     *
     * @param admin 管理员信息
     * @return 结果
     */
    int insert(SysAdmin admin);

    /**
     * 更新管理员状态
     *
     * @param admin 管理员信息
     * @return 结果
     */
    int updateById(SysAdmin admin);
}
