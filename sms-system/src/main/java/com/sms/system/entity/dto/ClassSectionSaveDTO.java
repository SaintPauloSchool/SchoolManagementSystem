package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 班級對照新增/修改請求
 */
public class ClassSectionSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String classSectionDsedj;
    private String classSectionSp;
    private Integer division;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
