package com.sms.system.entity.vo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 學生企微班級部門 ID 映射結果
 */
public class SysStudentMatchDeptMapVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, List<Long>> studentDeptMap = new HashMap<>();

    public Map<String, List<Long>> getStudentDeptMap() {
        return studentDeptMap;
    }

    public void setStudentDeptMap(Map<String, List<Long>> studentDeptMap) {
        this.studentDeptMap = studentDeptMap;
    }
}
