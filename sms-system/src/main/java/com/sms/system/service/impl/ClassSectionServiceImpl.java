package com.sms.system.service.impl;

import com.sms.system.entity.ClassSection;
import com.sms.system.mapper.ClassSectionMapper;
import com.sms.system.service.IClassSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 课程班级 Service 业务层处理
 */
@Service
public class ClassSectionServiceImpl implements IClassSectionService {

    @Autowired
    private ClassSectionMapper classSectionMapper;

    /**
     * 根据 DSEDJ 班级名称查询班级信息
     *
     * @param classSectionDsedj DSEDJ 班级名称
     * @return 班级信息
     */
    @Override
    public ClassSection getByDsedjName(String classSectionDsedj) {
        return classSectionMapper.selectByDsedjName(classSectionDsedj);
    }
}
