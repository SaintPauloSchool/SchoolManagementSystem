package com.sms.system.entity.vo;

import java.io.Serializable;
import java.util.Date;

/**
 * 學生數據匹配列表 VO（匹配記錄 + student_profiles.student_info 學籍資料）
 */
public class SysStudentMatchVO implements Serializable {
    private static final long serialVersionUID = 1L;

    // ── sys_student_match ──
    private Long id;
    private String userId;
    private Integer matchStatus;
    private Date createTime;
    private Date updateTime;

    // ── student_profiles.student_info ──
    private String studentId;
    private Integer inSchool;
    private String schoolYear;
    private Long studentProfileNumber;
    private String adid;
    private String classSection;
    private String classNum;
    private String idName;
    private String dsejStudentId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(Integer matchStatus) {
        this.matchStatus = matchStatus;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Integer getInSchool() {
        return inSchool;
    }

    public void setInSchool(Integer inSchool) {
        this.inSchool = inSchool;
    }

    public String getSchoolYear() {
        return schoolYear;
    }

    public void setSchoolYear(String schoolYear) {
        this.schoolYear = schoolYear;
    }

    public Long getStudentProfileNumber() {
        return studentProfileNumber;
    }

    public void setStudentProfileNumber(Long studentProfileNumber) {
        this.studentProfileNumber = studentProfileNumber;
    }

    public String getAdid() {
        return adid;
    }

    public void setAdid(String adid) {
        this.adid = adid;
    }

    public String getClassSection() {
        return classSection;
    }

    public void setClassSection(String classSection) {
        this.classSection = classSection;
    }

    public String getClassNum() {
        return classNum;
    }

    public void setClassNum(String classNum) {
        this.classNum = classNum;
    }

    public String getIdName() {
        return idName;
    }

    public void setIdName(String idName) {
        this.idName = idName;
    }

    public String getDsejStudentId() {
        return dsejStudentId;
    }

    public void setDsejStudentId(String dsejStudentId) {
        this.dsejStudentId = dsejStudentId;
    }
}
