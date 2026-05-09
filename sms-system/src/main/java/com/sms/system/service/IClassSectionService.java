package com.sms.system.service;

import com.sms.system.entity.ClassSection;

/**
 * 课程班级 Service 接口
 */
public interface IClassSectionService {

    /**
     * 根据 DSEDJ 班级名称查询班级信息
     *
     * @param classSectionDsedj DSEDJ 班级名称
     * @return 班级信息
     */
    ClassSection getByDsedjName(String classSectionDsedj);
}
