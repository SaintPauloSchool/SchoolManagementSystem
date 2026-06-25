package com.sms.system.entity.vo;

/**
 * 學生照片代理結果（Service 與 Controller 之間傳遞圖片字節，非配置項）
 */
public class StudentPhotoVO {

    private final byte[] data;
    private final String contentType;

    public StudentPhotoVO(byte[] data, String contentType) {
        this.data = data;
        this.contentType = contentType;
    }

    public byte[] getData() {
        return data;
    }

    public String getContentType() {
        return contentType;
    }
}
