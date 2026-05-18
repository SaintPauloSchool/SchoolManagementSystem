package com.sms.system.mapper;

import com.sms.system.entity.SysDepartmentParentBinding;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门家长绑定 Mapper 接口
 *
 */
public interface SysDepartmentParentBindingMapper {

    /**
     * 根据部门 ID 列表批量查询家长绑定列表
     *
     * @param departmentIds 部门 ID 列表
     * @return 家长绑定集合
     */
    List<SysDepartmentParentBinding> selectByDepartmentIds(@Param("departmentIds") List<Long> departmentIds);

    /**
     * 根据学生用户 ID 列表批量查询家长绑定列表
     *
     * @param studentUserIds 学生用户 ID 列表
     * @return 家长绑定集合
     */
    List<SysDepartmentParentBinding> selectByStudentUserIds(@Param("studentUserIds") List<String> studentUserIds);

    /**
     * 根据家长用户ID和学生用户ID批量查询部门绑定关系
     *
     * @param parentUserIds 家长用户 ID 列表
     * @param studentUserIds 学生用户 ID 列表
     * @return 部门绑定关系集合
     */
    List<SysDepartmentParentBinding> selectByParentAndStudentUserIds(
            @Param("parentUserIds") List<String> parentUserIds,
            @Param("studentUserIds") List<String> studentUserIds);

}
