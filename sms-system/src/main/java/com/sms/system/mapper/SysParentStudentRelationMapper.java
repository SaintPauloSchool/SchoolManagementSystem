package com.sms.system.mapper;

import com.sms.system.entity.SysParentStudentRelation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 家長學生關係 Mapper 接口
 *
 */
public interface SysParentStudentRelationMapper {

    /**
     * 批量查詢家長學生關係
     *
     * @param parentUserIds 家長用戶 ID 列表
     * @return 家長學生關係集合
     */
    List<SysParentStudentRelation> selectByParentUserIds(@Param("parentUserIds") List<String> parentUserIds);

    /**
     * 根據 ID 批量查詢家長學生關係
     *
     * @param ids 家長學生關係 ID 列表
     * @return 家長學生關係集合
     */
    List<SysParentStudentRelation> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 根據家長用戶ID和學生用戶ID批量查詢家長學生關係
     *
     * @param parentUserIds 家長用戶 ID 列表
     * @param studentUserIds 學生用戶 ID 列表
     * @return 家長學生關係集合
     */
    List<SysParentStudentRelation> selectByParentAndStudentUserIds(
            @Param("parentUserIds") List<String> parentUserIds,
            @Param("studentUserIds") List<String> studentUserIds);


    /**
     * 批量插入家長學生關係
     *
     * @param relations 關係列表
     * @return 影響行數
     */
    int batchInsert(@Param("relations") List<SysParentStudentRelation> relations);

    /**
     * 更新家長學生關係的可更新欄位
     *
     * @param relation 關係實體
     * @return 影響行數
     */
    int updateRelation(SysParentStudentRelation relation);

    /**
     * 批量刪除家長學生關係
     *
     * @param relations 關係列表
     * @return 影響行數
     */
    int deleteBatch(@Param("relations") List<SysParentStudentRelation> relations);

    /**
     * 全局清理已不在部門綁定中的家長學生關係記錄
     *
     * @return 影響行數
     */
    int deleteOrphanRelations();

    /**
     * 查詢所有家長學生關係記錄
     *
     * @return 關係列表
     */
    List<SysParentStudentRelation> selectAllRelations();

}
