package com.sms.system.service;

import com.sms.system.entity.ClassSection;

/**
 * 課程班級 Service 接口
 */
public interface IClassSectionService {

    /**
     * 根據 DSEDJ 班級名稱查詢班級信息
     *
     * @param classSectionDsedj DSEDJ 班級名稱
     * @return 班級信息
     */
    ClassSection getByDsedjName(String classSectionDsedj);
}
