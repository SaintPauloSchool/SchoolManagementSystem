package com.sms.system.mapper;

import com.sms.system.entity.SysDepartmentAdmin;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门管理员 Mapper 接口
 */
public interface SysDepartmentAdminMapper {

    /**
     * 根据企业微信 userid 查询该用户管理的所有部门管理员记录
     *
     * @param userid 企业微信 userid
     * @return 部门管理员记录列表
     */
    List<SysDepartmentAdmin> selectByUserid(@Param("userid") String userid);
}
