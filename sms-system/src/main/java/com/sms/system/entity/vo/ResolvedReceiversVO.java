package com.sms.system.entity.vo;

import com.sms.system.entity.SysSchoolFamilyContact;
import com.sms.system.entity.notification.receiver.NotificationReceiverTarget;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 解析後的通知接收者結果。
 * <p>企微家校通知僅向 {@link #parentUserIds} 發送。</p>
 */
public class ResolvedReceiversVO {

    private List<String> parentUserIds;
    private List<SysSchoolFamilyContact> relations;
    private List<NotificationReceiverTarget> receiverTargets;

    public ResolvedReceiversVO() {
    }

    public ResolvedReceiversVO(List<String> parentUserIds,
                               List<SysSchoolFamilyContact> relations,
                               List<NotificationReceiverTarget> receiverTargets) {
        this.parentUserIds = parentUserIds;
        this.relations = relations;
        this.receiverTargets = receiverTargets;
    }

    public boolean hasAnyReceiver() {
        return parentUserIds != null && !parentUserIds.isEmpty();
    }

    public List<String> getParentUserIds() {
        return parentUserIds;
    }

    public void setParentUserIds(List<String> parentUserIds) {
        this.parentUserIds = parentUserIds;
    }

    public List<SysSchoolFamilyContact> getRelations() {
        return relations;
    }

    public void setRelations(List<SysSchoolFamilyContact> relations) {
        this.relations = relations;
    }

    public List<NotificationReceiverTarget> getReceiverTargets() {
        return receiverTargets;
    }

    public void setReceiverTargets(List<NotificationReceiverTarget> receiverTargets) {
        this.receiverTargets = receiverTargets;
    }

    /** 解析過程中的可變上下文，解析完成後 {@link #build()} 得到 VO。 */
    public static class ResolutionContext {
        public final Set<String> parentUserIds = new LinkedHashSet<>();
        public final List<SysSchoolFamilyContact> relations = new ArrayList<>();
        public final List<NotificationReceiverTarget> receiverTargets = new ArrayList<>();
        public final Set<String> relationKeys = new HashSet<>();

        public ResolvedReceiversVO build() {
            return new ResolvedReceiversVO(
                    new ArrayList<>(parentUserIds),
                    new ArrayList<>(relations),
                    new ArrayList<>(receiverTargets));
        }
    }
}
