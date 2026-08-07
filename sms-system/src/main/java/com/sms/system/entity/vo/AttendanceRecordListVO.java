package com.sms.system.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sms.common.annotation.Excel;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考勤機記錄列表 VO（考勤記錄 + student_profiles.student_info 學籍資料）
 */
public class AttendanceRecordListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "學生 ID", sort = 5, width = 20)
    private String employeeId;

    @Excel(name = "拍卡時間", sort = 1, width = 22, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime accessDatetime;

    @Excel(name = "結果", sort = 11, readConverterExp = "0=失敗,1=成功")
    private String accessResult;

    @Excel(name = "裝置", sort = 8)
    private String deviceName;

    @Excel(name = "資源", sort = 9)
    private String resourceName;

    private String cardReaderName;

    @Excel(name = "拍卡人員", sort = 10)
    private String personName;

    @Excel(name = "方向", sort = 7, readConverterExp = "0=進入,1=離開")
    private String direction;

    @Excel(name = "卡號", sort = 6, width = 18, cellType = Excel.ColumnType.TEXT)
    private String cardNumber;

    @Excel(name = "班級", sort = 2)
    private String classSection;

    @Excel(name = "班號", sort = 3)
    private String classNum;

    @Excel(name = "姓名", sort = 4)
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
