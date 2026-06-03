package com.sms.system.mapper;

import com.sms.system.entity.WecomSchoolDepartment;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * wecom學校部門 Mapper 接口
 *
 */
public interface WecomSchoolDepartmentMapper {

    /**
     * 查詢所有學校部門（按排序值排序）
     *
     * @return 學校部門集合
     */
    List<WecomSchoolDepartment> selectAll();

    /**
     * 批量新增學校部門
     */
    int batchInsertSchoolDepartments(@Param("list") List<WecomSchoolDepartment> list);

    /**
     * 更新學校部門
     */
    int updateSchoolDepartment(WecomSchoolDepartment department);

    /**
     * 根據ID刪除學校部門
     */
    int deleteSchoolDepartmentById(Long id);

}
