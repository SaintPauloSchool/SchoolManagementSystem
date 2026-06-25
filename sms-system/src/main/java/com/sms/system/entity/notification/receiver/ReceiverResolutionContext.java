package com.sms.system.entity.notification.receiver;

import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.entity.vo.ResolvedReceiversVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 接收人解析過程中的可變上下文，避免在多個私有方法間傳遞大量參數。
 * <p>解析完成後通過 {@link #toResult(List)} 封裝為 {@link ResolvedReceiversVO}。</p>
 */
public class ReceiverResolutionContext {

    /** 實際發送目標：企微家長 userid 集合（去重） */
    public final Set<String> parentUserIds = new HashSet<>();
    /** 學生 userid 集合（當前業務未使用，保留擴展） */
    public final Set<String> studentUserIds = new HashSet<>();
    /** 部門 party 集合（當前業務未使用，保留擴展） */
    public final Set<String> partyIds = new HashSet<>();
    /** 企微選人產生的家長-學生-班級綁定列表 */
    public final List<SysDepartmentParentBinding> bindings = new ArrayList<>();
    /** 家長 userid → 所屬部門 ID（閱讀記錄、統計用） */
    public final Map<String, Long> studentDepartmentIds = new HashMap<>();
    /** 家長 userid → 學生 userid（自定義家校閱讀記錄關聯用） */
    public final Map<String, String> parentStudentUserIds = new HashMap<>();
    /** 解析過程中的綁定去重鍵集合 */
    public final Set<String> bindingKeys = new HashSet<>();

    public ResolvedReceiversVO toResult() {
        return new ResolvedReceiversVO(
                new ArrayList<>(parentUserIds),
                new ArrayList<>(studentUserIds),
                new ArrayList<>(partyIds),
                bindings,
                studentDepartmentIds,
                parentStudentUserIds
        );
    }

    public ResolvedReceiversVO toResult(List<SysDepartmentParentBinding> uniqueBindings) {
        return new ResolvedReceiversVO(
                new ArrayList<>(parentUserIds),
                new ArrayList<>(studentUserIds),
                new ArrayList<>(partyIds),
                uniqueBindings,
                studentDepartmentIds,
                parentStudentUserIds
        );
    }
}
