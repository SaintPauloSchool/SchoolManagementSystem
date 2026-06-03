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
     * 新增家長學生關係
     *
     * @param sysParentStudentRelation 家長學生關係
     * @return 結果
     */
    int insertIgnore(SysParentStudentRelation sysParentStudentRelation);

}
