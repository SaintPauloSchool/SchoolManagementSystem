package com.sms.system.constant;

/**
 * 系統配置鍵常量
 */
public final class SysConfigKeys {

    private SysConfigKeys() {
    }

    /** 家校通訊錄：使用的學段部門 ID（type=3，單選） */
    public static final String ADDRESS_BOOK_SEGMENT_DEPT_ID = "addressbook.segment_department_id";

    /** 每日學生手冊通知：發送範圍班級部門 ID 列表（type=1，多選，逗號分隔存儲） */
    public static final String DAILY_NOTICE_CLASS_DEPT_IDS = "notice.daily_class_department_ids";
}
