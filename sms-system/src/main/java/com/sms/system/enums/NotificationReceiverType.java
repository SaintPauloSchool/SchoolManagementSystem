package com.sms.system.enums;

import org.springframework.util.StringUtils;

/**
 * 通知接收來源類型（對應 notification_receiver.receive_type）
 */
public enum NotificationReceiverType {

    /** WeCom 家校通訊錄 */
    WECOM("1", "WeCom家校通訊錄"),
    /** 自定義家校通訊錄 */
    CUSTOM("2", "自定義家校通訊錄");

    private final String code;
    private final String label;

    NotificationReceiverType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static NotificationReceiverType fromCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        for (NotificationReceiverType type : values()) {
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
