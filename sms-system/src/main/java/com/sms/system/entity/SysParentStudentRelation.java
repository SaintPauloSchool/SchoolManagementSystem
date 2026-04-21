package com.sms.system.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.sms.common.core.domain.BaseEntity;

/**
 * 家长学生关系表 sys_parent_student_relation
 * 
 */
public class SysParentStudentRelation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    private Long id;

    /** 家长用户 ID */
    private String parentUserId;

    /** 学生用户 ID */
    private String studentUserId;

    /** 学生姓名 */
    private String studentName;

    /** 关系描述 */
    private String relationDesc;

    /** 家长手机号 */
    private String mobile;

    /** 家长外部用户 ID */
    private String externalUserid;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getParentUserId()
    {
        return parentUserId;
    }

    public void setParentUserId(String parentUserId)
    {
        this.parentUserId = parentUserId;
    }

    public String getStudentUserId()
    {
        return studentUserId;
    }

    public void setStudentUserId(String studentUserId)
    {
        this.studentUserId = studentUserId;
    }

    public String getStudentName()
    {
        return studentName;
    }

    public void setStudentName(String studentName)
    {
        this.studentName = studentName;
    }

    public String getRelationDesc()
    {
        return relationDesc;
    }

    public void setRelationDesc(String relationDesc)
    {
        this.relationDesc = relationDesc;
    }

    public String getMobile()
    {
        return mobile;
    }

    public void setMobile(String mobile)
    {
        this.mobile = mobile;
    }

    public String getExternalUserid()
    {
        return externalUserid;
    }

    public void setExternalUserid(String externalUserid)
    {
        this.externalUserid = externalUserid;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
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
