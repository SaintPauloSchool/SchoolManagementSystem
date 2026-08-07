package com.sms.system.entity.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 通知接收對象按部門平鋪分組（展示用，非持久化）。
 */
public class NotificationReceiverDeptGroupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long departmentId;
    private String departmentName;
    private Integer count;
    private List<String> names;

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }
}
