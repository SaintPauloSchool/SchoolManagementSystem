package com.sms.system.entity.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 未回复学生信息 VO
 * 用于提示家长回复功能
 *
 */
public class UnrepliedStudentVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 学生用户ID
     */
    private String studentUserId;

    /**
     * 未回复的家长用户ID列表
     */
    private List<String> parentUserIds;

    public UnrepliedStudentVO() {
    }

    public UnrepliedStudentVO(String studentUserId, List<String> parentUserIds) {
        this.studentUserId = studentUserId;
        this.parentUserIds = parentUserIds;
    }

    public String getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(String studentUserId) {
        this.studentUserId = studentUserId;
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
                "studentUserId='" + studentUserId + '\'' +
                ", parentUserIds=" + parentUserIds +
                '}';
    }
}
