package com.sms.system.entity;

import java.io.Serializable;

/**
 * 班級對照實體類
 */
public class ClassSection implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主鍵 ID */
    private Long id;

    /** DSEDJ 班級名稱（如：P1_A_家長） */
    private String classSectionDsedj;

    /** SP 班級名稱（如：P1A） */
    private String classSectionSp;

    /** 學部（0幼稚園 1小學 2中學） */
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

    @Override
    public String toString() {
        return "ClassSection{" +
                "id=" + id +
                ", classSectionDsedj='" + classSectionDsedj + '\'' +
                ", classSectionSp='" + classSectionSp + '\'' +
                ", division=" + division +
                '}';
    }
}
