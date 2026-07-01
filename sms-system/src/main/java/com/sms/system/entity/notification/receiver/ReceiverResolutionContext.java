package com.sms.system.entity.notification.receiver;

import com.sms.system.entity.SysSchoolFamilyContact;
import com.sms.system.entity.vo.ResolvedReceiversVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 接收人解析過程中的可變上下文，避免在多個私有方法間傳遞大量參數。
 */
public class ReceiverResolutionContext {

    public final Set<String> parentUserIds = new HashSet<>();
    public final Set<String> studentUserIds = new HashSet<>();
    public final Set<String> partyIds = new HashSet<>();
    public final List<SysSchoolFamilyContact> relations = new ArrayList<>();
    public final Map<String, Long> studentDepartmentIds = new HashMap<>();
    public final Map<String, String> parentStudentUserIds = new HashMap<>();
    public final Set<String> relationKeys = new HashSet<>();

    public ResolvedReceiversVO toResult() {
        return new ResolvedReceiversVO(
                new ArrayList<>(parentUserIds),
                new ArrayList<>(studentUserIds),
                new ArrayList<>(partyIds),
                relations,
                studentDepartmentIds,
                parentStudentUserIds
        );
    }

    public ResolvedReceiversVO toResult(List<SysSchoolFamilyContact> uniqueRelations) {
        return new ResolvedReceiversVO(
                new ArrayList<>(parentUserIds),
                new ArrayList<>(studentUserIds),
                new ArrayList<>(partyIds),
                uniqueRelations,
                studentDepartmentIds,
                parentStudentUserIds
        );
    }
}
