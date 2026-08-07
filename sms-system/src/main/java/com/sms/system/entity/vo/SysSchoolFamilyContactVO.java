package com.sms.system.entity.vo;

import java.io.Serializable;

/**
 * 家校通訊錄聯絡人 VO（含關聯查詢字段，非表字段通過 JOIN 填充）
 */
public class SysSchoolFamilyContactVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long departmentId;
    private String parentUserId;
    private String studentUserId;
    /** 學籍學生 ID（關聯 student_profiles.student_info.student_id，非 sys_school_family_contact 表字段） */
    private String studentId;
    private String studentName;
    private String mobile;
    private String relationDesc;
    private String classCodeWecom;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getParentUserId() { return parentUserId; }
    public void setParentUserId(String parentUserId) { this.parentUserId = parentUserId; }

    public String getStudentUserId() { return studentUserId; }
    public void setStudentUserId(String studentUserId) { this.studentUserId = studentUserId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getRelationDesc() { return relationDesc; }
    public void setRelationDesc(String relationDesc) { this.relationDesc = relationDesc; }

    public String getClassCodeWecom() { return classCodeWecom; }
    public void setClassCodeWecom(String classCodeWecom) { this.classCodeWecom = classCodeWecom; }
}
