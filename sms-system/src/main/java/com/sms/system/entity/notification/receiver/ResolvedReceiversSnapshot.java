package com.sms.system.entity.notification.receiver;

import com.sms.system.entity.SysSchoolFamilyContact;
import com.sms.system.entity.vo.ResolvedReceiversVO;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 解析後接收人的不可變快照，供發佈流程使用。
 */
public final class ResolvedReceiversSnapshot {

    private final List<String> parentUserIds;
    private final List<String> studentUserIds;
    private final List<String> partyIds;
    private final List<SysSchoolFamilyContact> relations;
    private final Map<String, Long> studentDepartmentIds;
    private final Map<String, String> parentStudentUserIds;

    private ResolvedReceiversSnapshot(List<String> parentUserIds,
                                        List<String> studentUserIds,
                                        List<String> partyIds,
                                        List<SysSchoolFamilyContact> relations,
                                        Map<String, Long> studentDepartmentIds,
                                        Map<String, String> parentStudentUserIds) {
        this.parentUserIds = parentUserIds;
        this.studentUserIds = studentUserIds;
        this.partyIds = partyIds;
        this.relations = relations;
        this.studentDepartmentIds = studentDepartmentIds;
        this.parentStudentUserIds = parentStudentUserIds;
    }

    public static ResolvedReceiversSnapshot from(ResolvedReceiversVO resolved) {
        if (resolved == null) {
            return empty();
        }
        return new ResolvedReceiversSnapshot(
                nullSafeList(resolved.getParentUserIds()),
                nullSafeList(resolved.getStudentUserIds()),
                nullSafeList(resolved.getPartyIds()),
                nullSafeList(resolved.getRelations()),
                resolved.getStudentDepartmentIds() != null
                        ? resolved.getStudentDepartmentIds() : Collections.emptyMap(),
                resolved.getParentStudentUserIds() != null
                        ? resolved.getParentStudentUserIds() : Collections.emptyMap()
        );
    }

    public static ResolvedReceiversSnapshot empty() {
        return new ResolvedReceiversSnapshot(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyMap()
        );
    }

    private static <T> List<T> nullSafeList(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }

    public boolean hasAnyReceiver() {
        return !parentUserIds.isEmpty() || !studentUserIds.isEmpty() || !partyIds.isEmpty();
    }

    public List<String> getParentUserIds() {
        return parentUserIds;
    }

    public List<String> getStudentUserIds() {
        return studentUserIds;
    }

    public List<String> getPartyIds() {
        return partyIds;
    }

    public List<SysSchoolFamilyContact> getRelations() {
        return relations;
    }

    public Map<String, Long> getStudentDepartmentIds() {
        return studentDepartmentIds;
    }

    public Map<String, String> getParentStudentUserIds() {
        return parentStudentUserIds;
    }
}
