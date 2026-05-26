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

    /**
     * 根据部门ID和userid查询
     * @param departmentId 部门ID
     * @param userid 用户ID
     * @return 部门管理员记录
     */
    SysDepartmentAdmin selectByDepartmentIdAndUserid(@Param("departmentId") Long departmentId, @Param("userid") String userid);

    /**
     * 批量插入部门管理员
     * @param admins 管理员列表
     * @return 结果
     */
    int batchInsertDepartmentAdmins(@Param("admins") List<SysDepartmentAdmin> admins);

    /**
     * 根据部门ID和userid更新
     * @param admin 管理员记录
     * @return 结果
     */
    int updateByDepartmentIdAndUserid(SysDepartmentAdmin admin);
}
