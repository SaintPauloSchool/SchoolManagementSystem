package com.sms.common.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 全局反序列化工具類：
 * 將前端傳來的純日期字串（yyyy-MM-dd）或完整日期時間字串（yyyy-MM-dd HH:mm:ss）
 * 統一轉為 LocalDateTime，純日期自動補 00:00:00。
 *
 * 使用方式：在需要的欄位上加 @JsonDeserialize(using = LocalDateToDateTimeDeserializer.class)
 */
public class LocalDateToDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

    public LocalDateToDateTimeDeserializer() {
        super(LocalDateTime.class);
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        value = value.trim();
        // 如果只有日期部分（yyyy-MM-dd，共 10 個字元），補上時間 00:00:00
        if (value.length() == 10) {
            return LocalDate.parse(value).atStartOfDay();
        }
        // 否則當作完整的 LocalDateTime 解析（相容 "yyyy-MM-dd HH:mm:ss" 與 "yyyy-MM-ddTHH:mm:ss"）
        return LocalDateTime.parse(value.replace(" ", "T"));
    }
}
