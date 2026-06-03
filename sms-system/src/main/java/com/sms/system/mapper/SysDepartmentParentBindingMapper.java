package com.sms.system.mapper;

import com.sms.system.entity.SysDepartmentParentBinding;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部門家長綁定 Mapper 接口
 *
 */
public interface SysDepartmentParentBindingMapper {

    /**
     * 根據部門 ID 列表批量查詢家長綁定列表
     *
     * @param departmentIds 部門 ID 列表
     * @return 家長綁定集合
     */
    List<SysDepartmentParentBinding> selectByDepartmentIds(@Param("departmentIds") List<Long> departmentIds);

    /**
     * 根據學生用戶 ID 列表批量查詢家長綁定列表
     *
     * @param studentUserIds 學生用戶 ID 列表
     * @return 家長綁定集合
     */
    List<SysDepartmentParentBinding> selectByStudentUserIds(@Param("studentUserIds") List<String> studentUserIds);

    /**
     * 根據家長用戶ID和學生用戶ID批量查詢部門綁定關係
     *
     * @param parentUserIds 家長用戶 ID 列表
     * @param studentUserIds 學生用戶 ID 列表
     * @return 部門綁定關係集合
     */
    List<SysDepartmentParentBinding> selectByParentAndStudentUserIds(
            @Param("parentUserIds") List<String> parentUserIds,
            @Param("studentUserIds") List<String> studentUserIds);

    /**
     * 獲取所有家長用戶ID
     * @return 家長用戶ID列表
     */
    List<String> selectAllParentUserIds();

    /**
     * 根據部門ID查詢
     * @param departmentId 部門ID
     * @return 部門綁定關係集合
     */
    List<SysDepartmentParentBinding> selectByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * 插入綁定關係
     * @param binding 部門綁定關係
     * @return 結果
     */
    int insertIgnore(SysDepartmentParentBinding binding);

    /**
     * 更新綁定關係
     * @param binding 部門綁定關係
     * @return 結果
     */
    int updateById(SysDepartmentParentBinding binding);

    /**
     * 根據ID刪除
     * @param id 綁定ID
     * @return 結果
     */
    int deleteById(@Param("id") Long id);

}
