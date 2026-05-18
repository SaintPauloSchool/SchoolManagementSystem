package com.sms.system.mapper;

import com.sms.system.entity.SysParentStudentRelation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 家长学生关系 Mapper 接口
 *
 */
public interface SysParentStudentRelationMapper {

    /**
     * 批量查询家长学生关系
     *
     * @param parentUserIds 家长用户 ID 列表
     * @return 家长学生关系集合
     */
    List<SysParentStudentRelation> selectByParentUserIds(@Param("parentUserIds") List<String> parentUserIds);

    /**
     * 根据 ID 批量查询家长学生关系
     *
     * @param ids 家长学生关系 ID 列表
     * @return 家长学生关系集合
     */
    List<SysParentStudentRelation> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 根据家长用户ID和学生用户ID批量查询家长学生关系
     *
     * @param parentUserIds 家长用户 ID 列表
     * @param studentUserIds 学生用户 ID 列表
     * @return 家长学生关系集合
     */
    List<SysParentStudentRelation> selectByParentAndStudentUserIds(
            @Param("parentUserIds") List<String> parentUserIds,
            @Param("studentUserIds") List<String> studentUserIds);

}
