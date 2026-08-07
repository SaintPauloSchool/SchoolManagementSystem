package com.sms.system.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.sms.common.core.domain.BaseEntity;

/**
 * 家校通訊錄聯絡人表 sys_school_family_contact
 */
public class SysSchoolFamilyContact extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long departmentId;
    private String parentUserId;
    private String studentUserId;
    private String studentName;
    private String relationDesc;
    private String mobile;
    private String externalUserid;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getParentUserId() { return parentUserId; }
    public void setParentUserId(String parentUserId) { this.parentUserId = parentUserId; }

    public String getStudentUserId() { return studentUserId; }
    public void setStudentUserId(String studentUserId) { this.studentUserId = studentUserId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getRelationDesc() { return relationDesc; }
    public void setRelationDesc(String relationDesc) { this.relationDesc = relationDesc; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getExternalUserid() { return externalUserid; }
    public void setExternalUserid(String externalUserid) { this.externalUserid = externalUserid; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("departmentId", getDepartmentId())
            .append("parentUserId", getParentUserId())
            .append("studentUserId", getStudentUserId())
            .append("studentName", getStudentName())
            .append("relationDesc", getRelationDesc())
            .append("mobile", getMobile())
            .append("externalUserid", getExternalUserid())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
