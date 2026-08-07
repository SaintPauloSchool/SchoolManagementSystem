package com.sms.system.entity.notification.receiver;

import com.sms.system.entity.notification.NotificationUserReadRecord;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 通知發送/重發按學籍維度統計的共用工具。 */
public final class NotificationReceiverStats {

    private NotificationReceiverStats() {
    }

    /** 有學籍 ID 時按 student_id 統計；否則按家長 userid 單獨計數。 */
    public static String studentStatsKey(String studentId, String parentUserId) {
        if (StringUtils.hasText(studentId)) {
            return studentId.trim();
        }
        return "parent:" + parentUserId.trim();
    }

    public static String studentStatsKey(NotificationReceiverTarget target) {
        return studentStatsKey(target.getStudentId(), target.getParentUserId());
    }

    public static String studentStatsKey(NotificationUserReadRecord record) {
        return studentStatsKey(record.getStudentId(), record.getUserId());
    }

    public static Map<String, Set<String>> groupParentsByStudent(List<NotificationReceiverTarget> targets) {
        Map<String, Set<String>> studentToParents = new HashMap<>();
        if (targets == null) {
            return studentToParents;
        }
        for (NotificationReceiverTarget target : targets) {
            if (!StringUtils.hasText(target.getParentUserId())) {
                continue;
            }
            studentToParents.computeIfAbsent(studentStatsKey(target), k -> new HashSet<>())
                    .add(target.getParentUserId());
        }
        return studentToParents;
    }

    public static Map<String, Set<String>> groupParentsByStudentFromReadRecords(
            List<NotificationUserReadRecord> records) {
        Map<String, Set<String>> studentToParents = new HashMap<>();
        if (records == null) {
            return studentToParents;
        }
        for (NotificationUserReadRecord record : records) {
            if (!StringUtils.hasText(record.getUserId())) {
                continue;
            }
            studentToParents.computeIfAbsent(studentStatsKey(record), k -> new HashSet<>())
                    .add(record.getUserId());
        }
        return studentToParents;
    }

    /** @return int[]{successCount, failCount} */
    public static int[] countStudentResults(Map<String, Set<String>> studentToParents, Set<String> successUserIds) {
        int successCount = 0;
        int failCount = 0;
        for (Set<String> parentIds : studentToParents.values()) {
            if (isAnyParentSendSuccess(parentIds, successUserIds)) {
                successCount++;
            } else {
                failCount++;
            }
        }
        return new int[]{successCount, failCount};
    }

    public static boolean isAnyParentSendSuccess(Set<String> parentIds, Set<String> successUserIds) {
        if (parentIds == null || successUserIds == null) {
            return false;
        }
        for (String parentId : parentIds) {
            if (successUserIds.contains(parentId)) {
                return true;
            }
        }
        return false;
    }
}
