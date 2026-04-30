package com.sms.system.entity.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 问题项 VO
 * 用于通知导出功能中的问题数据封装
 */
public class QuestionItemVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 问题ID
     */
    private Long id;
    
    /**
     * 问题标题
     */
    private String title;
    
    /**
     * 问题类型
     */
    private String type;
    
    /**
     * 选项列表
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
