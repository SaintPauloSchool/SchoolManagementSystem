package com.sms.system.entity.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 系統學校部門成員批量新增請求
 */
public class SysSchoolDepartmentMemberBatchSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<SysSchoolDepartmentMemberSaveDTO> members;
    private Integer type;

    public List<SysSchoolDepartmentMemberSaveDTO> getMembers() {
        return members;
    }

    public void setMembers(List<SysSchoolDepartmentMemberSaveDTO> members) {
        this.members = members;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
