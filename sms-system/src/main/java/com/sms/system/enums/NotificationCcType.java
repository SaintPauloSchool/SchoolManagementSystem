package com.sms.system.enums;

import org.springframework.util.StringUtils;

/**
 * 通知抄送來源類型（對應 notification_cc.cc_type）
 */
public enum NotificationCcType {

    /** WeCom 老師通訊錄 */
    WECOM("1", "WeCom老師通訊錄"),
    /** 自定義老師通訊錄 */
    CUSTOM("2", "自定義老師通訊錄");

    private final String code;
    private final String label;

    NotificationCcType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static NotificationCcType fromCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        for (NotificationCcType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static boolean isSupported(String code) {
        return fromCode(code) != null;
    }
}
