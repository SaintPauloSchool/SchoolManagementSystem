package com.sms.system.mapper;

import com.sms.system.entity.ClassSection;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 班級對照 Mapper 接口
 */
public interface ClassSectionMapper {

    /**
     * 根據 DSEDJ 班級名稱查詢班級資訊
     */
    ClassSection selectByDsedjName(@Param("classSectionDsedj") String classSectionDsedj);

    /**
     * 同時查詢 DSEDJ 班級或 SP 班級是否已存在
     */
    List<ClassSection> selectDuplicateList(@Param("classSectionDsedj") String classSectionDsedj,
                                           @Param("classSectionSp") String classSectionSp,
                                           @Param("excludeId") Long excludeId);

    /**
     * 根據 ID 查詢班級資訊
     */
    ClassSection selectClassSectionById(Long id);

    /**
     * 查詢班級列表
     */
    List<ClassSection> selectClassSectionList(ClassSection classSection);

    /**
     * 新增班級
     */
    int insertClassSection(ClassSection classSection);

    /**
     * 修改班級
     */
    int updateClassSection(ClassSection classSection);

    /**
     * 批量刪除班級
     */
    int deleteClassSectionByIds(@Param("ids") Long[] ids);
}
