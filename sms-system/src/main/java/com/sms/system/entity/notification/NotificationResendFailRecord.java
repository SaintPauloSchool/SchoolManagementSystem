package com.sms.system.entity.notification;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知重发失败记录实体类
 * 用于追踪每个用户的重发失败情况，失败次数达到 3 次则放弃重发
 */
@TableName("notification_resend_fail_record")
public class NotificationResendFailRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 通知ID */
    @TableField("notification_id")
    private Long notificationId;

    /** 发送记录ID */
    @TableField("send_record_id")
    private Long sendRecordId;

    /** 接收用户ID（家长或学生） */
    @TableField("user_id")
    private String userId;

    /** 用户类型（1学生 2家长） */
    @TableField("user_type")
    private String userType;

    /** 关联学生ID */
    @TableField("student_user_id")
    private String studentUserId;

    /** 第1次失败原因 */
    @TableField("fail_reason_1")
    private String failReason1;

    /** 第1次失败详细信息 */
    @TableField("fail_message_1")
    private String failMessage1;

    /** 第2次失败原因 */
    @TableField("fail_reason_2")
    private String failReason2;

    /** 第2次失败详细信息 */
    @TableField("fail_message_2")
    private String failMessage2;

    /** 第3次失败原因 */
    @TableField("fail_reason_3")
    private String failReason3;

    /** 第3次失败详细信息 */
    @TableField("fail_message_3")
    private String failMessage3;

    /** 累计失败次数（最大 3 次） */
    @TableField("fail_count")
    private Integer failCount;

    /** 状态：0-待重发 1-已放弃 */
    @TableField("status")
    private String status;

    /** 首次失败时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private Date createTime;

    /** 最近更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private Date updateTime;

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }

    public Long getSendRecordId() { return sendRecordId; }
    public void setSendRecordId(Long sendRecordId) { this.sendRecordId = sendRecordId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getStudentUserId() { return studentUserId; }
    public void setStudentUserId(String studentUserId) { this.studentUserId = studentUserId; }

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

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return "NotificationResendFailRecord{" +
                "id=" + id +
                ", notificationId=" + notificationId +
                ", sendRecordId=" + sendRecordId +
                ", userId='" + userId + '\'' +
                ", userType='" + userType + '\'' +
                ", studentUserId='" + studentUserId + '\'' +
                ", failReason1='" + failReason1 + '\'' +
                ", failReason2='" + failReason2 + '\'' +
                ", failReason3='" + failReason3 + '\'' +
                ", failCount=" + failCount +
                ", status='" + status + '\'' +
                '}';
    }
}
