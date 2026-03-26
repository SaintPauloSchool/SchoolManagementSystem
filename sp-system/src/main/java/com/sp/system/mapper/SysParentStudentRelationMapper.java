package com.sp.system.mapper;

import com.sp.system.entity.SysParentStudentRelation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 家长学生关系 Mapper 接口
 *
 */
public interface SysParentStudentRelationMapper {

    /**
     * 根据家长用户 ID 查询关系列表
     *
     * @param parentUserId 家长用户 ID
     * @return 家长学生关系集合
     */
    List<SysParentStudentRelation> selectByParentUserId(@Param("parentUserId") String parentUserId);

    /**
     * 批量查询家长学生关系
     *
     * @param parentUserIds 家长用户 ID 列表
     * @return 家长学生关系集合
     */
    List<SysParentStudentRelation> selectByParentUserIds(@Param("parentUserIds") List<String> parentUserIds);
}
