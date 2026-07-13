package com.sms.system.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考勤機記錄列表 VO（考勤記錄 + student_profiles.student_info 學籍資料）
 */
public class AttendanceRecordListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String employeeId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime accessDatetime;
    private String accessResult;
    private String deviceName;
    private String resourceName;
    private String cardReaderName;
    private String personName;
    private String direction;
    private String cardNumber;
    private String classSection;
    private String classNum;
    private String idName;

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

    public String getAccessResult() {
        return accessResult;
    }

    public void setAccessResult(String accessResult) {
        this.accessResult = accessResult;
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

    public String getCardReaderName() {
        return cardReaderName;
    }

    public void setCardReaderName(String cardReaderName) {
        this.cardReaderName = cardReaderName;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
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
}
