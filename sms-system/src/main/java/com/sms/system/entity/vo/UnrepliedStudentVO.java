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
     * 學生用戶ID
     */
    private String studentUserId;

    /**
     * 未回復的家長用戶ID列表
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
