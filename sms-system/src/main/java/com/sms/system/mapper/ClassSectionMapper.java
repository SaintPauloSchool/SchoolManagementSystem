package com.sms.system.mapper;

import com.sms.system.entity.ClassSection;
import org.apache.ibatis.annotations.Param;

/**
 * 课程班级 Mapper 接口
 */
public interface ClassSectionMapper {

    /**
     * 根据 DSEDJ 班级名称查询班级信息
     *
     * @param classSectionDsedj DSEDJ 班级名称
     * @return 班级信息
     */
    ClassSection selectByDsedjName(@Param("classSectionDsedj") String classSectionDsedj);
}
