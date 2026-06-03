package com.sms.system.entity;

import java.io.Serializable;

/**
 * 課程班級實體類
 */
public class ClassSection implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主鍵 ID */
    private Long id;

    /** DSEDJ 班級名稱（如：P1_A_家長） */
    private String classSectionDsedj;

    /** SP 班級名稱（如：P1A） */
    private String classSectionSp;

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

    @Override
    public String toString() {
        return "ClassSection{" +
                "id=" + id +
                ", classSectionDsedj='" + classSectionDsedj + '\'' +
                ", classSectionSp='" + classSectionSp + '\'' +
                '}';
    }
}
