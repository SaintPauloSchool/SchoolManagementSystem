package com.sms.system.service.impl;

import com.sms.system.entity.ClassSection;
import com.sms.system.mapper.ClassSectionMapper;
import com.sms.system.service.IClassSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 課程班級 Service 業務層處理
 */
@Service
public class ClassSectionServiceImpl implements IClassSectionService {

    @Autowired
    private ClassSectionMapper classSectionMapper;

    /**
     * 根據 DSEDJ 班級名稱查詢班級信息
     *
     * @param classSectionDsedj DSEDJ 班級名稱
     * @return 班級信息
     */
    @Override
    public ClassSection getByDsedjName(String classSectionDsedj) {
        return classSectionMapper.selectByDsedjName(classSectionDsedj);
    }
}
