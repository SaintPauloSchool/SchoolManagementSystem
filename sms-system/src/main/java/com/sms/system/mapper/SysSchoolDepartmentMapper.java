package com.sms.system.mapper;

import com.sms.system.entity.SysSchoolDepartment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系統學校部門 Mapper 接口
 *
 */
public interface SysSchoolDepartmentMapper {

    /**
     * 查詢當前用戶擁有的學校部門（按排序值排序）
     *
     * @param type         部門類型（1 學校部門 2 家校通訊錄）
     * @param ownerUserid  擁有者企微 userid
     * @return 學校部門集合
     */
    List<SysSchoolDepartment> selectAll(@Param("type") Integer type, @Param("ownerUserid") String ownerUserid);

    /**
     * 根據 ID 刪除學校部門
     *
     * @param id 部門 ID
     * @return 結果
     */
    int deleteById(Long id);

    /**
     * 根據 ID 批量刪除學校部門
     *
     * @param ids 部門 ID 數組
     * @return 結果
     */
    int deleteByIds(@Param("ids") Long[] ids);

    /**
     * 插入部門資訊
     *
     * @param department 部門資訊
     * @return 結果
     */
    int insertDepartment(SysSchoolDepartment department);

    /**
     * 修改部門資訊
     *
     * @param department 部門資訊
     * @return 結果
     */
    int updateDepartment(SysSchoolDepartment department);

    /**
     * 根據 ID 查詢部門
     *
     * @param id 部門 ID
     * @return 部門資訊
     */
    SysSchoolDepartment selectById(Long id);

}
