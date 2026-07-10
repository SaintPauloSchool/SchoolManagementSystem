package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 班級對照查詢條件
 */
public class ClassSectionQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String classSectionDsedj;
    private String classSectionSp;
    private Integer division;

    public String getClassSectionDsedj() {
        return classSectionDsedj;
    }

    public void setClassSectionDsedj(String classSectionDsedj) {
        this.classSectionDsedj = classSectionDsedj;
    }

    public String getClassSectionSp() {
        return classSectionSp;
    }

    public void setClassSectionSp(String classSectionSp) {
        this.classSectionSp = classSectionSp;
    }

    public Integer getDivision() {
        return division;
    }

    public void setDivision(Integer division) {
        this.division = division;
    }
}
