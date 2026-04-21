package com.sms.system.mapper;

import com.sms.system.entity.SysDepartment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门 Mapper 接口
 *
 */
public interface SysDepartmentMapper {

    /**
     * 查询所有部门（按类型和排序值排序）
     *
     * @return 部门集合
     */
    List<SysDepartment> selectAll();
}
