package com.sms.system.entity.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 未回復學生信息 VO
 * 用於提示家長回復功能
 *
 */
public class UnrepliedStudentVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 學籍 student_id
     */
    private String studentId;

    /**
     * 未回復的家長用戶ID列表
     */
    private List<String> parentUserIds;

    public UnrepliedStudentVO() {
    }

    public UnrepliedStudentVO(String studentId, List<String> parentUserIds) {
        this.studentId = studentId;
        this.parentUserIds = parentUserIds;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public List<String> getParentUserIds() {
        return parentUserIds;
    }

    public void setParentUserIds(List<String> parentUserIds) {
        this.parentUserIds = parentUserIds;
    }

    @Override
    public String toString() {
        return "UnrepliedStudentVO{" +
                "studentId='" + studentId + '\'' +
                ", parentUserIds=" + parentUserIds +
                '}';
    }
}
