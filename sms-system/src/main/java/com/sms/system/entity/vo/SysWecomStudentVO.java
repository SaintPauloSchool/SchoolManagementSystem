package com.sms.system.entity.vo;

import java.io.Serializable;

/**
 * 企業微信學生信息 VO
 *
 */
public class SysWecomStudentVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 學生用戶ID (企微)
     */
    private String studentUserId;

    /**
     * 學生姓名 (企微)
     */
    private String studentName;

    /**
     * 家長手機號
     */
    private String mobile;

    /**
     * 家長關係
     */
    private String relationDesc;

    /**
     * 班級代碼 (企微對照)
     */
    private String classCodeWecom;

    public SysWecomStudentVO() {
    }

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
        return "SysWecomStudentVO{" +
                "studentUserId='" + studentUserId + '\'' +
                ", studentName='" + studentName + '\'' +
                ", mobile='" + mobile + '\'' +
                ", relationDesc='" + relationDesc + '\'' +
                ", classCodeWecom='" + classCodeWecom + '\'' +
                '}';
    }
}
