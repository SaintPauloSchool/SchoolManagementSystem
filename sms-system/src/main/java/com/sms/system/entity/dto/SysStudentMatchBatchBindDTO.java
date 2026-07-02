package com.sms.system.entity.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 批量手動綁定學生匹配請求
 */
public class SysStudentMatchBatchBindDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 學籍 student_id */
    private String studentId;
    /** 家長企微 parent_user_id 列表 */
    private List<String> userIds;

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }
}
