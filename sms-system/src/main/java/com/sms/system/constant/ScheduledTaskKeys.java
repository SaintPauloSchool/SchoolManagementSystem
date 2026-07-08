package com.sms.system.constant;

/**
 * 定時任務唯一標識（對應 sys_scheduled_task.task_key）
 */
public final class ScheduledTaskKeys {

    private ScheduledTaskKeys() {
    }

    public static final String FAILED_TASK_NOTIFIER = "failed_task_notifier";
    public static final String DEPARTMENT_SYNC = "department_sync";
    public static final String NOTIFICATION_REMINDER = "notification_reminder";
    public static final String NOTIFICATION_RESEND = "notification_resend";
    public static final String SCHOOL_FAMILY_CONTACT_SYNC = "school_family_contact_sync";
    public static final String SCHOOL_NOTICE = "school_notice";
    public static final String WECOM_SCHOOL_DEPARTMENT = "wecom_school_department";
    public static final String ATTENDANCE_NOTIFY = "attendance_notify";
}
