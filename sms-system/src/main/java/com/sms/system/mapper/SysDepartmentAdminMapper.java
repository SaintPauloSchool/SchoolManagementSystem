package com.sms.system.mapper;

import com.sms.system.entity.SysDepartmentAdmin;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部門管理員 Mapper 接口
 */
public interface SysDepartmentAdminMapper {

    /**
     * 根據企業微信 userid 查詢該用戶管理的所有部門管理員記錄
     *
     * @param userid 企業微信 userid
     * @return 部門管理員記錄列表
     */
    List<SysDepartmentAdmin> selectByUserid(@Param("userid") String userid);

    /**
     * 根據部門ID和userid查詢
     * @param departmentId 部門ID
     * @param userid 用戶ID
     * @return 部門管理員記錄
     */
    SysDepartmentAdmin selectByDepartmentIdAndUserid(@Param("departmentId") Long departmentId, @Param("userid") String userid);

    /**
     * 批量插入部門管理員
     * @param admins 管理員列表
     * @return 結果
     */
    int batchInsertDepartmentAdmins(@Param("admins") List<SysDepartmentAdmin> admins);

    /**
     * 根據部門ID和userid更新
     * @param admin 管理員記錄
     * @return 結果
     */
    int updateByDepartmentIdAndUserid(SysDepartmentAdmin admin);
}
