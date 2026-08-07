package com.sms.system.service;

import com.sms.system.entity.ClassSection;
import com.sms.system.entity.dto.ClassSectionQueryDTO;
import com.sms.system.entity.dto.ClassSectionSaveDTO;

import java.util.List;

/**
 * 班級對照 Service 接口
 */
public interface IClassSectionService {

    /**
     * 根據 DSEDJ 班級名稱查詢班級資訊
     */
    ClassSection getByDsedjName(String classSectionDsedj);

    /**
     * 根據 ID 查詢班級資訊
     */
    ClassSection selectClassSectionById(Long id);

    /**
     * 查詢班級列表
     */
    List<ClassSection> selectClassSectionList(ClassSectionQueryDTO queryDTO);

    /**
     * 新增班級
     */
    int insertClassSection(ClassSectionSaveDTO saveDTO);

    /**
     * 修改班級
     */
    int updateClassSection(ClassSectionSaveDTO saveDTO);

    /**
     * 批量刪除班級
     */
    int deleteClassSectionByIds(Long[] ids);
}
