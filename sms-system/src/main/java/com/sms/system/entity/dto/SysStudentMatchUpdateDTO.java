package com.sms.system.entity.dto;

import java.io.Serializable;

/**
 * 更正已匹配記錄的家長 user_id
 */
public class SysStudentMatchUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 匹配記錄主鍵 */
    private Long id;
    /** 新的家長企微 parent_user_id */
    private String userId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
