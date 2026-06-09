package com.sms.system.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.sms.common.core.domain.BaseEntity;

/**
 * 學生數據匹配實體類 sys_student_match
 *
 */
public class SysStudentMatch extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主鍵 ID */
    private Long id;

    /** 學生個人編號 (來自 Excel) */
    private String studentProfileNum;

    /** 學生帳號 (來自 Excel) */
    private String adid;

    /** 學校準確姓名 (來自 Excel) */
    private String studentNameLocal;

    /** 學校準確班級代碼 (來自 Excel) */
    private String classNameLocal;

    /** 身份證英文名 (來自 Excel) */
    private String idEnglishName;

    /** 英文名 (來自 Excel) */
    private String englishFirstName;

    /** 英文姓 (來自 Excel) */
    private String englishLastName;

    /** 匹配到的企微學生 UserID */
    private String studentUserIdWecom;

    /** 匹配到的企微原始學生姓名 */
    private String studentNameWecom;

    /** 匹配狀態 (0: 未匹配, 1: 自動匹配成功, 2: 手動匹配成功) */
    private String matchStatus;

    /** 企微同步狀態 (0: 未同步, 1: 同步成功, 2: 同步失敗) */
    private String syncStatus;

    /** 同步失敗的具體原因 */
    private String errorMsg;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentProfileNum() {
        return studentProfileNum;
    }

    public void setStudentProfileNum(String studentProfileNum) {
        this.studentProfileNum = studentProfileNum;
    }

    public String getAdid() {
        return adid;
    }

    public void setAdid(String adid) {
        this.adid = adid;
    }

    public String getStudentNameLocal() {
        return studentNameLocal;
    }

    public void setStudentNameLocal(String studentNameLocal) {
        this.studentNameLocal = studentNameLocal;
    }

    public String getClassNameLocal() {
        return classNameLocal;
    }

    public void setClassNameLocal(String classNameLocal) {
        this.classNameLocal = classNameLocal;
    }

    public String getIdEnglishName() {
        return idEnglishName;
    }

    public void setIdEnglishName(String idEnglishName) {
        this.idEnglishName = idEnglishName;
    }

    public String getEnglishFirstName() {
        return englishFirstName;
    }

    public void setEnglishFirstName(String englishFirstName) {
        this.englishFirstName = englishFirstName;
    }

    public String getEnglishLastName() {
        return englishLastName;
    }

    public void setEnglishLastName(String englishLastName) {
        this.englishLastName = englishLastName;
    }

    public String getStudentUserIdWecom() {
        return studentUserIdWecom;
    }

    public void setStudentUserIdWecom(String studentUserIdWecom) {
        this.studentUserIdWecom = studentUserIdWecom;
    }

    public String getStudentNameWecom() {
        return studentNameWecom;
    }

    public void setStudentNameWecom(String studentNameWecom) {
        this.studentNameWecom = studentNameWecom;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("studentProfileNum", getStudentProfileNum())
            .append("adid", getAdid())
            .append("studentNameLocal", getStudentNameLocal())
            .append("classNameLocal", getClassNameLocal())
            .append("idEnglishName", getIdEnglishName())
            .append("englishFirstName", getEnglishFirstName())
            .append("englishLastName", getEnglishLastName())
            .append("studentUserIdWecom", getStudentUserIdWecom())
            .append("studentNameWecom", getStudentNameWecom())
            .append("matchStatus", getMatchStatus())
            .append("syncStatus", getSyncStatus())
            .append("errorMsg", getErrorMsg())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
