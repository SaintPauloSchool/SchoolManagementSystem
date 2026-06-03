package com.sms.system.entity.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 問題項 VO
 * 用於通知導出功能中的問題數據封裝
 */
public class QuestionItemVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 問題ID
     */
    private Long id;
    
    /**
     * 問題標題
     */
    private String title;
    
    /**
     * 問題類型
     */
    private String type;
    
    /**
     * 選項列表
     */
    private List<String> options;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}
