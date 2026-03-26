package com.sp.system.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.sp.common.core.domain.BaseEntity;

/**
 * 部门家长绑定表 sys_department_parent_binding
 * 
 */
public class SysDepartmentParentBinding extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    private Long id;

    /** 部门 ID */
    private Long departmentId;

    /** 家长用户 ID */
    private String parentUserId;

    /** 学生用户 ID */
    private String studentUserId;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getDepartmentId()
    {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId)
    {
        this.departmentId = departmentId;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("departmentId", getDepartmentId())
            .append("parentUserId", getParentUserId())
            .append("studentUserId", getStudentUserId())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
