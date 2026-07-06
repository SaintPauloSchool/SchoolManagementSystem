package com.sms.system.entity.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系統學校部門成員 VO
 */
public class SysSchoolDepartmentMemberVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String userid;
    private String name;
    private Long schoolDepartmentId;
    private Long departmentId;
    private String openUserid;
    private String studentId;
    private Integer type;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSchoolDepartmentId() {
        return schoolDepartmentId;
    }

    public void setSchoolDepartmentId(Long schoolDepartmentId) {
        this.schoolDepartmentId = schoolDepartmentId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getOpenUserid() {
        return openUserid;
    }

    public void setOpenUserid(String openUserid) {
        this.openUserid = openUserid;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
