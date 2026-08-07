package com.sms.system.enums;

/**
 * 學生匹配狀態（對應 sys_student_match.match_status）
 */
public enum StudentMatchStatus {

    /** 未匹配（僅列表查詢條件，不入庫） */
    UNMATCHED(0, "未匹配"),
    /** 自動匹配成功 */
    AUTO(1, "自動匹配成功"),
    /** 手動匹配成功 */
    MANUAL(2, "手動匹配成功");

    private final int code;
    private final String label;

    StudentMatchStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static StudentMatchStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (StudentMatchStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
