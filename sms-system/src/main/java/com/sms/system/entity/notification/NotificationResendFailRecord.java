package com.sms.system.entity.notification;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知重發失敗記錄實體類
 * 用於追蹤每個用戶的重發失敗情況，失敗次數達到 3 次則放棄重發
 */
@TableName("notification_resend_fail_record")
public class NotificationResendFailRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主鍵ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 通知ID */
    @TableField("notification_id")
    private Long notificationId;

    /** 發送記錄ID */
    @TableField("send_record_id")
    private Long sendRecordId;

    /** 接收用戶ID（家長 userid，僅學生/家長重發失敗記錄） */
    @TableField("user_id")
    private String userId;

    /** 關聯學籍 student_id */
    @TableField("student_id")
    private String studentId;

    /** 第1次失敗原因 */
    @TableField("fail_reason_1")
    private String failReason1;

    /** 第1次失敗詳細資訊 */
    @TableField("fail_message_1")
    private String failMessage1;

    /** 第2次失敗原因 */
    @TableField("fail_reason_2")
    private String failReason2;

    /** 第2次失敗詳細資訊 */
    @TableField("fail_message_2")
    private String failMessage2;

    /** 第3次失敗原因 */
    @TableField("fail_reason_3")
    private String failReason3;

    /** 第3次失敗詳細資訊 */
    @TableField("fail_message_3")
    private String failMessage3;

    /** 累計失敗次數（最大 3 次） */
    @TableField("fail_count")
    private Integer failCount;

    /** 狀態：0-待重發 1-已放棄 */
    @TableField("status")
    private String status;

    /** 首次失敗時間 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 最近更新時間 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }

    public Long getSendRecordId() { return sendRecordId; }
    public void setSendRecordId(Long sendRecordId) { this.sendRecordId = sendRecordId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getFailReason1() { return failReason1; }
    public void setFailReason1(String failReason1) { this.failReason1 = failReason1; }

    public String getFailMessage1() { return failMessage1; }
    public void setFailMessage1(String failMessage1) { this.failMessage1 = failMessage1; }

    public String getFailReason2() { return failReason2; }
    public void setFailReason2(String failReason2) { this.failReason2 = failReason2; }

    public String getFailMessage2() { return failMessage2; }
    public void setFailMessage2(String failMessage2) { this.failMessage2 = failMessage2; }

    public String getFailReason3() { return failReason3; }
    public void setFailReason3(String failReason3) { this.failReason3 = failReason3; }

    public String getFailMessage3() { return failMessage3; }
    public void setFailMessage3(String failMessage3) { this.failMessage3 = failMessage3; }

    public Integer getFailCount() { return failCount; }
    public void setFailCount(Integer failCount) { this.failCount = failCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return "NotificationResendFailRecord{" +
                "id=" + id +
                ", notificationId=" + notificationId +
                ", sendRecordId=" + sendRecordId +
                ", userId='" + userId + '\'' +
                ", studentId='" + studentId + '\'' +
                ", failReason1='" + failReason1 + '\'' +
                ", failReason2='" + failReason2 + '\'' +
                ", failReason3='" + failReason3 + '\'' +
                ", failCount=" + failCount +
                ", status='" + status + '\'' +
                '}';
    }
}
