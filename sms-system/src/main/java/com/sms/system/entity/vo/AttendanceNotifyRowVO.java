package com.sms.system.entity.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考勤拍卡通知待發送行（考勤記錄 + 學籍 + 家長 user_id）
 */
public class AttendanceNotifyRowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String employeeId;
    private LocalDateTime accessDatetime;
    private String deviceName;
    private String resourceName;
    private String personName;
    private String classSection;
    private String idName;
    private String direction;
    private String parentUserId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDateTime getAccessDatetime() {
        return accessDatetime;
    }

    public void setAccessDatetime(LocalDateTime accessDatetime) {
        this.accessDatetime = accessDatetime;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getClassSection() {
        return classSection;
    }

    public void setClassSection(String classSection) {
        this.classSection = classSection;
    }

    public String getIdName() {
        return idName;
    }

    public void setIdName(String idName) {
        this.idName = idName;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getParentUserId() {
        return parentUserId;
    }

    public void setParentUserId(String parentUserId) {
        this.parentUserId = parentUserId;
    }
}
