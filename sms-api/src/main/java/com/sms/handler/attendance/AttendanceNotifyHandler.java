package com.sms.handler.attendance;

import com.alibaba.fastjson.JSONObject;
import com.sms.common.config.StudentProfilesProperties;
import com.sms.framework.wechat.WechatWorkHttpClient;
import com.sms.handler.notification.NotificationSchoolSendHelper;
import com.sms.system.entity.task.TaskResult;
import com.sms.system.entity.vo.AttendanceNotifyRowVO;
import com.sms.system.mapper.AttendanceRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 考勤拍卡微信通知處理器。
 */
@Component
public class AttendanceNotifyHandler {

    private static final Logger log = LoggerFactory.getLogger(AttendanceNotifyHandler.class);

    /** 每次任務最多處理的考勤記錄數，避免單次執行時間過長 */
    private static final int QUERY_LIMIT = 200;

    /** 拍卡時間格式化模板，對應 {@code access_datetime} 欄位 */
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;

    @Autowired
    private StudentProfilesProperties studentProfilesProperties;

    @Autowired
    private NotificationSchoolSendHelper schoolSendHelper;

    @Autowired
    private WechatWorkHttpClient wechatWorkHttpClient;

    /**
     * 掃描未通知考勤記錄，向匹配家長發送微信家校通知。
     *
     * @return 任務執行結果（成功數、失敗數、摘要訊息）
     */
    public TaskResult processPendingNotifications() {
        // student_profiles 為跨庫關聯，需傳入配置的資料庫名稱
        String studentProfilesDatabase = studentProfilesProperties.getDatabase();
        List<AttendanceNotifyRowVO> rows = attendanceRecordMapper.selectPendingNotifyRows(
                studentProfilesDatabase, QUERY_LIMIT);
        if (rows == null || rows.isEmpty()) {
            return TaskResult.success(0, 0, "無待通知考勤記錄");
        }

        // 同一考勤記錄可能 JOIN 出多行（每位家長一行），按記錄 ID 分組後逐條處理
        Map<Long, List<AttendanceNotifyRowVO>> groupedByRecord = rows.stream()
                .collect(Collectors.groupingBy(
                        AttendanceNotifyRowVO::getId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<Long, List<AttendanceNotifyRowVO>> entry : groupedByRecord.entrySet()) {
            Long recordId = entry.getKey();
            List<AttendanceNotifyRowVO> recordRows = entry.getValue();
            if (recordRows.isEmpty()) {
                continue;
            }

            Set<String> parentUserIds = collectParentUserIds(recordRows);
            if (parentUserIds.isEmpty()) {
                // 學生未綁定家長，跳過並計入失敗（下次任務重試）
                log.warn("考勤記錄 {} 未找到可通知的家長 user_id", recordId);
                failCount++;
                continue;
            }

            String content = buildNotifyContent(recordRows.get(0));
            try {
                // 僅發送給家長（to_parent_userid），不走老師通道
                JSONObject payload = schoolSendHelper.buildParentOnlyPayload(
                        new ArrayList<>(parentUserIds), content);
                JSONObject result = wechatWorkHttpClient.sendSchoolNotification(payload);
                if (!isWechatSuccess(result)) {
                    log.error("考勤記錄 {} 通知發送失敗: {}", recordId, result);
                    failCount++;
                    continue;
                }

                // 企微發送成功後才更新通知狀態，避免漏發
                int updated = attendanceRecordMapper.markNotified(recordId);
                if (updated > 0) {
                    successCount++;
                    log.info("考勤記錄 {} 通知已發送給 {} 位家長", recordId, parentUserIds.size());
                } else {
                    // 併發場景下可能已被其他任務實例標記
                    log.warn("考勤記錄 {} 通知發送成功但更新 is_notified 失敗（可能已被其他任務處理）", recordId);
                    failCount++;
                }
            } catch (Exception e) {
                log.error("考勤記錄 {} 通知發送異常", recordId, e);
                failCount++;
            }
        }

        String message = String.format("處理 %d 條考勤記錄，成功 %d，失敗 %d",
                groupedByRecord.size(), successCount, failCount);
        return TaskResult.success(successCount, failCount, message);
    }

    /**
     * 從同一考勤記錄的多行 JOIN 結果中彙總去重後的家長 user_id。
     *
     * @param recordRows 同一 {@code attendance_record.id} 對應的多行資料
     * @return 去重後的家長 user_id 集合（保持插入順序）
     */
    private Set<String> collectParentUserIds(List<AttendanceNotifyRowVO> recordRows) {
        Set<String> parentUserIds = new LinkedHashSet<>();
        for (AttendanceNotifyRowVO row : recordRows) {
            if (StringUtils.hasText(row.getParentUserId())) {
                parentUserIds.add(row.getParentUserId().trim());
            }
        }
        return parentUserIds;
    }

    /**
     * 組裝微信通知正文。
     * <p>格式：{@code {班別} {姓名} 在 {拍卡時間} 在 {裝置名稱} 的 {資源名稱} 拍卡}</p>
     *
     * @param row 考勤記錄行（含學生班別、姓名及拍卡資訊）
     * @return 通知正文
     */
    private String buildNotifyContent(AttendanceNotifyRowVO row) {
        String classSection = firstNonBlank(row.getClassSection(), "");
        String studentName = firstNonBlank(row.getIdName(), row.getPersonName(), "學生");
        String accessDatetime = row.getAccessDatetime() != null
                ? row.getAccessDatetime().format(DATETIME_FORMATTER)
                : "";
        String deviceName = firstNonBlank(row.getDeviceName(), "未知裝置");
        String resourceName = firstNonBlank(row.getResourceName(), "未知位置");

        String prefix = StringUtils.hasText(classSection)
                ? classSection + " " + studentName
                : studentName;
        return prefix + " 在 " + accessDatetime + " 在 " + deviceName + " 的 " + resourceName + " 拍卡";
    }

    /**
     * 返回第一個非空字串；全部為空時返回空字串。
     *
     * @param values 候選字串（按優先順序）
     * @return 第一個非空值（已 trim），或 {@code ""}
     */
    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    /**
     * 判斷企業微信接口回應是否成功。
     *
     * @param result 企微 API 回應 JSON
     * @return {@code errcode == 0} 時為 {@code true}
     */
    private boolean isWechatSuccess(JSONObject result) {
        if (result == null) {
            return false;
        }
        Integer errcode = result.getInteger("errcode");
        return errcode != null && errcode == 0;
    }
}
