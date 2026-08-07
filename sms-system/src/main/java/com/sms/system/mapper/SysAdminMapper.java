package com.sms.system.mapper;

import com.sms.system.entity.SysAdmin;
import com.sms.system.entity.dto.SysAdminQueryDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系統管理員 Mapper 接口
 */
public interface SysAdminMapper {

    /**
     * 獲取所有狀態正常的管理員用戶ID列表
     */
    List<String> selectAdminUserIds();

    /**
     * 根據用戶ID查詢管理員資訊
     */
    SysAdmin selectByUserId(@Param("userId") String userId);

    /**
     * 根據主鍵查詢
     */
    SysAdmin selectById(@Param("id") Long id);

    /**
     * 條件查詢列表
     */
    List<SysAdmin> selectList(SysAdminQueryDTO queryDTO);

    /**
     * 統計指定類型且狀態正常的數量
     */
    int countByTypeAndStatus(@Param("type") String type, @Param("status") String status);

    /**
     * 新增管理員
     */
    int insert(SysAdmin admin);

    /**
     * 更新管理員
     */
    int updateById(SysAdmin admin);

    /**
     * 按主鍵刪除
     */
    int deleteById(@Param("id") Long id);

    /**
     * 批量刪除
     */
    int deleteByIds(@Param("ids") Long[] ids);
}
