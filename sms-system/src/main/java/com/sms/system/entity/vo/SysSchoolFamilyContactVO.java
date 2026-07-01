package com.sms.system.entity.vo;

import java.io.Serializable;

/**
 * 家校通訊錄聯絡人 VO（含關聯班級代碼）
 */
public class SysSchoolFamilyContactVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String parentUserId;
    private String studentUserId;
    private String studentName;
    private String mobile;
    private String relationDesc;
    private String classCodeWecom;

    public String getParentUserId() { return parentUserId; }
    public void setParentUserId(String parentUserId) { this.parentUserId = parentUserId; }

    public String getStudentUserId() { return studentUserId; }
    public void setStudentUserId(String studentUserId) { this.studentUserId = studentUserId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getRelationDesc() { return relationDesc; }
    public void setRelationDesc(String relationDesc) { this.relationDesc = relationDesc; }

    public String getClassCodeWecom() { return classCodeWecom; }
    public void setClassCodeWecom(String classCodeWecom) { this.classCodeWecom = classCodeWecom; }
}
