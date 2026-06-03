package com.sms.system.entity.vo;

import java.util.List;

/**
 * 分批發送通知時的接收者承載實體 VO
 */
public class BatchReceiversVO {
    private final List<String> parentIds;
    private final List<String> studentIds;
    private final List<String> partyIds;

    public BatchReceiversVO(List<String> parentIds, List<String> studentIds, List<String> partyIds) {
        this.parentIds = parentIds;
        this.studentIds = studentIds;
        this.partyIds = partyIds;
    }

    public List<String> getParentIds() {
        return parentIds;
    }

    public List<String> getStudentIds() {
        return studentIds;
    }

    public List<String> getPartyIds() {
        return partyIds;
    }

    public boolean isEmpty() {
        return (parentIds == null || parentIds.isEmpty())
                && (studentIds == null || studentIds.isEmpty())
                && (partyIds == null || partyIds.isEmpty());
    }
}
