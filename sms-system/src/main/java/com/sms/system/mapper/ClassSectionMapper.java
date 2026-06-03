package com.sms.system.mapper;

import com.sms.system.entity.ClassSection;
import org.apache.ibatis.annotations.Param;

/**
 * 課程班級 Mapper 接口
 */
public interface ClassSectionMapper {

    /**
     * 根據 DSEDJ 班級名稱查詢班級信息
     *
     * @param classSectionDsedj DSEDJ 班級名稱
     * @return 班級信息
     */
    ClassSection selectByDsedjName(@Param("classSectionDsedj") String classSectionDsedj);
}
