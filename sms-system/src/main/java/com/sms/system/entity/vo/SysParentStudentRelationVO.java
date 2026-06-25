package com.sms.system.entity.vo;

import java.io.Serializable;

/**
 * 家長學生關係 VO（含關聯班級代碼）
 */
public class SysParentStudentRelationVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 學生用戶 ID（企微） */
    private String studentUserId;

    /** 學生姓名（企微） */
    private String studentName;

    /** 家長手機號 */
    private String mobile;

    /** 家長關係 */
    private String relationDesc;

    /** 班級代碼（關聯 class_section） */
    private String classCodeWecom;

    public String getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(String studentUserId) {
        this.studentUserId = studentUserId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getRelationDesc() {
        return relationDesc;
    }

    public void setRelationDesc(String relationDesc) {
        this.relationDesc = relationDesc;
    }

    public String getClassCodeWecom() {
        return classCodeWecom;
    }

    public void setClassCodeWecom(String classCodeWecom) {
        this.classCodeWecom = classCodeWecom;
    }

    @Override
    public String toString() {
        return "SysParentStudentRelationVO{" +
                "studentUserId='" + studentUserId + '\'' +
                ", studentName='" + studentName + '\'' +
                ", mobile='" + mobile + '\'' +
                ", relationDesc='" + relationDesc + '\'' +
                ", classCodeWecom='" + classCodeWecom + '\'' +
                '}';
    }
}
