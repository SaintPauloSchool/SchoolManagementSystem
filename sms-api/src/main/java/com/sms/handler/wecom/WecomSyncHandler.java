package com.sms.handler.wecom;

import com.sms.system.entity.task.TaskResult;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.framework.wechat.WechatWorkHttpClient;
import com.sms.system.service.IWecomSchoolDepartmentService;
import com.sms.system.service.ISysDepartmentService;
import com.sms.system.service.ISysSchoolFamilyContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 企業微信數據同步處理器
 * 集中管理所有與企業微信相關的定時同步業務邏輯，
 * 供各定時任務調用，定時任務本身只負責觸發。
 */
@Component
public class WecomSyncHandler {

    private static final Logger log = LoggerFactory.getLogger(WecomSyncHandler.class);

    @Autowired
    private WechatWorkHttpClient wechatWorkHttpClient;

    @Autowired
    private IWecomSchoolDepartmentService wecomSchoolDepartmentService;

    @Autowired
    private ISysDepartmentService departmentService;

    @Autowired
    private ISysSchoolFamilyContactService schoolFamilyContactService;

    /**
     * 檢查 API 呼叫是否回傳錯誤
     */
    private boolean isApiError(JSONObject result) {
        return result != null && result.getInteger("errcode") != null && result.getInteger("errcode") != 0;
    }

    /**
     * 同步家校通訊錄部門與管理員數據
     * 由 DepartmentSyncTask 調用（每日凌晨 0 點）
     */
    public TaskResult syncSchoolDepartments() {
        log.info("開始同步家校通訊錄部門數據");

        JSONObject departmentJson = wechatWorkHttpClient.getSchoolDepartmentList();
        if (isApiError(departmentJson)) {
            return TaskResult.fail(0, 1, "獲取家校通訊錄部門失敗: " + departmentJson.getString("errmsg"));
        }
        departmentService.syncSchoolDepartmentData(departmentJson);

        log.info("家校通訊錄部門數據同步完成");
        return TaskResult.success(1, 0, "同步成功");
    }

    /**
     * 同步家校通訊錄家長聯絡人數據
     * 由 SchoolFamilyContactSyncTask 調用（每日凌晨 0 點 30 分）
     */
    public TaskResult syncSchoolFamilyContacts() {
        log.info("開始同步家校通訊錄");
        // 獲取部門數據
        List<Long> targetDepartmentIds = departmentService.getClassDepartmentId();
        if (targetDepartmentIds == null || targetDepartmentIds.isEmpty()) {
            log.error("未獲取到班級部門 ID，同步任務結束");
            return TaskResult.success(0, 0, "無班級部門需同步");
        }

        log.info("成功獲取到 {} 個目標部門 ID", targetDepartmentIds.size());
        int failCount = 0;
        String firstErrorReason = null;

        //遍歷部門數據
        for (Long targetDepartmentId : targetDepartmentIds) {
            log.info("開始執行部門 ID {} 的家校通訊錄同步", targetDepartmentId);
            // 獲取家校通訊錄家長列表
            JSONObject parentJson = wechatWorkHttpClient.getSchoolParentList(targetDepartmentId);

            //檢查 API 呼叫是否回傳錯誤
            if (isApiError(parentJson)) {
                failCount++;
                if (firstErrorReason == null) {
                    firstErrorReason = parentJson.getString("errmsg");
                }
                log.warn("部門 ID {} 拉取家校通訊錄失敗，跳過同步", targetDepartmentId);
                continue;
            }

            // 同步指定班級部門的企微家校通訊錄聯絡人數據
            schoolFamilyContactService.syncSchoolFamilyContactData(targetDepartmentId, parentJson);
        }

        if (failCount > 0) {
            return new TaskResult(targetDepartmentIds.size() - failCount, failCount,
                    "共 " + failCount + " 個數據同步失敗，原因: " + firstErrorReason);
        }

        log.info("家校通訊錄數據同步完成");
        return TaskResult.success(targetDepartmentIds.size(), 0, "同步成功");
    }

    /**
     * 同步企業微信部門與成員數據
     * 由 WecomSchoolDepartmentTask 調用（每日凌晨 1 點 30 分）
     */
    public TaskResult syncWecomDepartmentsAndMembers() {
        log.info("開始同步企業微信部門與成員數據");

        JSONObject departmentResult = wechatWorkHttpClient.getDepartmentList();
        if (isApiError(departmentResult)) {
            return TaskResult.fail(0, 1, "獲取企業微信部門失敗: " + departmentResult.getString("errmsg"));
        }
        wecomSchoolDepartmentService.syncWecomSchoolDepartments(departmentResult);

        int memberFailCount = 0;
        String firstMemberErrorReason = null;

        if (departmentResult != null && !isApiError(departmentResult)) {
            JSONArray departmentArray = departmentResult.getJSONArray("department");
            if (departmentArray != null && !departmentArray.isEmpty()) {
                Map<Long, JSONObject> departmentMembersMap = new HashMap<>();
                for (int i = 0; i < departmentArray.size(); i++) {
                    JSONObject deptObj = departmentArray.getJSONObject(i);
                    Long deptId = deptObj.getLong("id");
                    JSONObject memberResult = wechatWorkHttpClient.getDepartmentMembers(deptId);

                    if (isApiError(memberResult)) {
                        memberFailCount++;
                        if (firstMemberErrorReason == null) {
                            firstMemberErrorReason = memberResult.getString("errmsg");
                        }
                    } else {
                        departmentMembersMap.put(deptId, memberResult);
                    }
                }

                if (!departmentMembersMap.isEmpty()) {
                    wecomSchoolDepartmentService.syncWecomSchoolDepartmentMembersBatch(departmentMembersMap);
                }
            }
        }

        int totalDepts = departmentResult != null && departmentResult.getJSONArray("department") != null
                ? departmentResult.getJSONArray("department").size() : 0;

        if (memberFailCount > 0) {
            return new TaskResult(totalDepts - memberFailCount, memberFailCount,
                    "共 " + memberFailCount + " 個部門成員同步失敗，原因: " + firstMemberErrorReason);
        }

        log.info("企業微信部門與成員數據同步完成");
        return TaskResult.success(totalDepts, 0, "同步成功");
    }
}
