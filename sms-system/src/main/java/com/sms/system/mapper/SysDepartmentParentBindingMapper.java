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

    /**
     * 获取所有家长用户ID
     * @return 家长用户ID列表
     */
    List<String> selectAllParentUserIds();

    /**
     * 根据部门ID查询
     * @param departmentId 部门ID
     * @return 部门绑定关系集合
     */
    List<SysDepartmentParentBinding> selectByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * 插入绑定关系
     * @param binding 部门绑定关系
     * @return 结果
     */
    int insertIgnore(SysDepartmentParentBinding binding);

    /**
     * 更新绑定关系
     * @param binding 部门绑定关系
     * @return 结果
     */
    int updateById(SysDepartmentParentBinding binding);

    /**
     * 根据ID删除
     * @param id 绑定ID
     * @return 结果
     */
    int deleteById(@Param("id") Long id);

}
