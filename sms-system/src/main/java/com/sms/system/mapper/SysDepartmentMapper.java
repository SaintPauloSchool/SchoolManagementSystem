package com.sms.system.mapper;

import com.sms.system.entity.SysDepartment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部門 Mapper 接口
 *
 */
public interface SysDepartmentMapper {

    /**
     * 查詢所有部門（按類型和排序值排序）
     *
     * @return 部門集合
     */
    List<SysDepartment> selectAll();

    /**
     * 批量保存部門信息
     * @param departments 部門列表
     */
    void batchInsertDepartments(@Param("list") List<SysDepartment> departments);

    /**
     * 獲取班級部門 ID
     * @return 班級部門 ID 列表
     */
    List<Long> selectClassDepartmentId();
}
