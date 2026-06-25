package com.sms.system.entity.vo;

import com.sms.system.entity.SysDepartmentParentBinding;

import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * 解析後的通知接收者結果視圖對象
 */
public class ResolvedReceiversVO {
    
    /** 發送給家長的企業微信 userId 列表 */
    private List<String> parentUserIds;

    /** 發送給學生的企業微信 userId 列表 */
    private List<String> studentUserIds;

    /** 發送給部門的部門 ID 列表 */
    private List<String> partyIds;

    /** 從發送對象中解析出來的精確家長與學生綁定關係 */
    private List<SysDepartmentParentBinding> bindings;

    /** 學生 userId 對應的部門 ID */
    private Map<String, Long> studentDepartmentIds;

    /** 自定義家校成員：家長 userId → 學生 userId */
    private Map<String, String> parentStudentUserIds;

    public ResolvedReceiversVO() {
    }

    public ResolvedReceiversVO(List<String> parentUserIds, List<String> studentUserIds, List<String> partyIds,
                               List<SysDepartmentParentBinding> bindings, Map<String, Long> studentDepartmentIds,
                               Map<String, String> parentStudentUserIds) {
        this.parentUserIds = parentUserIds;
        this.studentUserIds = studentUserIds;
        this.partyIds = partyIds;
        this.bindings = bindings;
        this.studentDepartmentIds = studentDepartmentIds;
        this.parentStudentUserIds = parentStudentUserIds;
    }

    public ResolvedReceiversVO(List<String> parentUserIds, List<String> studentUserIds, List<String> partyIds,
                               List<SysDepartmentParentBinding> bindings, Map<String, Long> studentDepartmentIds) {
        this(parentUserIds, studentUserIds, partyIds, bindings, studentDepartmentIds, Collections.emptyMap());
    }

    public List<String> getParentUserIds() {
        return parentUserIds;
    }

    public void setParentUserIds(List<String> parentUserIds) {
        this.parentUserIds = parentUserIds;
    }

    public List<String> getStudentUserIds() {
        return studentUserIds;
    }

    public void setStudentUserIds(List<String> studentUserIds) {
        this.studentUserIds = studentUserIds;
    }

    public List<String> getPartyIds() {
        return partyIds;
    }

    public void setPartyIds(List<String> partyIds) {
        this.partyIds = partyIds;
    }

    public List<SysDepartmentParentBinding> getBindings() {
        return bindings;
    }

    public void setBindings(List<SysDepartmentParentBinding> bindings) {
        this.bindings = bindings;
    }

    public Map<String, Long> getStudentDepartmentIds() {
        return studentDepartmentIds;
    }

    public void setStudentDepartmentIds(Map<String, Long> studentDepartmentIds) {
        this.studentDepartmentIds = studentDepartmentIds;
    }

    public Map<String, String> getParentStudentUserIds() {
        return parentStudentUserIds;
    }

    public void setParentStudentUserIds(Map<String, String> parentStudentUserIds) {
        this.parentStudentUserIds = parentStudentUserIds;
    }
}
