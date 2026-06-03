package com.sms.system.mapper;

import com.sms.system.entity.SysAdmin;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系統管理員 Mapper 接口
 */
public interface SysAdminMapper {
    /**
     * 獲取所有狀態正常的管理員用戶ID列表
     *
     * @return 用戶ID列表
     */
    List<String> selectAdminUserIds();
    /**
     * 根據用戶ID查詢管理員信息
     *
     * @param userId 用戶ID
     * @return 管理員信息
     */
    SysAdmin selectByUserId(@Param("userId") String userId);

    /**
     * 新增管理員
     *
     * @param admin 管理員信息
     * @return 結果
     */
    int insert(SysAdmin admin);

    /**
     * 更新管理員狀態
     *
     * @param admin 管理員信息
     * @return 結果
     */
    int updateById(SysAdmin admin);
}
