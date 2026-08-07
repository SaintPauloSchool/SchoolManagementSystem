package com.sms.system.entity.notification.receiver;

import java.io.Serializable;

/**
 * 通知接收目標（家長 + 學籍學生），對應前端選人結果與 {@code receive_data} 單項。
 */
public class NotificationReceiverTarget implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 家長企微 userid（發送目標，寫入閱讀記錄 user_id） */
    private String parentUserId;

    /** 學籍 student_id（關聯 student_profiles.student_info.student_id） */
    private String studentId;

    /** 選人時所在班級/部門 ID（來自選擇框，寫入閱讀記錄 department_id） */
    private Long departmentId;

    /** 自定義家校通訊錄部門節點 ID（sys_school_department.id，僅展示分組用） */
    private Long schoolDepartmentId;

    public NotificationReceiverTarget() {
    }

    public NotificationReceiverTarget(String parentUserId, String studentId) {
        this.parentUserId = parentUserId;
        this.studentId = studentId;
    }

    public String getParentUserId() {
        return parentUserId;
    }

    public void setParentUserId(String parentUserId) {
        this.parentUserId = parentUserId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getSchoolDepartmentId() {
        return schoolDepartmentId;
    }

    public void setSchoolDepartmentId(Long schoolDepartmentId) {
        this.schoolDepartmentId = schoolDepartmentId;
    }
}
