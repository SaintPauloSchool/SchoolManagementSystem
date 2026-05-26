package com.sms.handler.wecom;

import com.sms.system.entity.task.TaskResult;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.framework.wechat.WechatWorkHttpClient;
import com.sms.system.service.IWecomSchoolDepartmentService;
import com.sms.system.service.ISysDepartmentService;
import com.sms.system.service.ISysParentStudentRelationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

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
    private ISysParentStudentRelationService parentStudentRelationService;

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
     * 同步家長學生關係數據
     * 由 ParentStudentRelationSyncTask 調用（每日凌晨 0 點 30 分）
     */
    public TaskResult syncParentStudentRelations() {
        log.info("開始同步家長學生關係");
        // 獲取所有班級部門 ID
        List<Long> targetDepartmentIds = departmentService.getClassDepartmentId();
        if (targetDepartmentIds == null || targetDepartmentIds.isEmpty()) {
            log.error("未獲取到班級部門 ID，同步任務結束");
            return TaskResult.success(0, 0, "無班級部門需同步");
        }

        log.info("成功獲取到 {} 個目標部門 ID", targetDepartmentIds.size());
        int failCount = 0;
        String firstErrorReason = null;
        
        // 遍歷所有班級部門 ID
        for (Long targetDepartmentId : targetDepartmentIds) {
            log.info("開始執行部門 ID {} 的家長學生關係同步", targetDepartmentId);
            // 呼叫企微家校家長列表api
            JSONObject parentJson = wechatWorkHttpClient.getSchoolParentList(targetDepartmentId);
            
            if (isApiError(parentJson)) {
                failCount++;
                if (firstErrorReason == null) {
                    firstErrorReason = parentJson.getString("errmsg");
                }
            }
            
            parentStudentRelationService.syncParentStudentRelationData(targetDepartmentId, parentJson);
        }

        if (failCount > 0) {
            return new TaskResult(targetDepartmentIds.size() - failCount, failCount, "共 " + failCount + " 個數據同步失敗，原因: " + firstErrorReason);
        }

        log.info("家長學生關係數據同步完成");
        return TaskResult.success(targetDepartmentIds.size(), 0, "同步成功");
    }

    /**
     * 同步企業微信部門與成員數據
     * 由 WecomSchoolDepartmentTask 調用（每日凌晨 1 點）
     */
    public TaskResult syncWecomDepartmentsAndMembers() {
        log.info("開始同步企業微信部門與成員數據");

        // 1. 獲取並同步部門列表
        JSONObject departmentResult = wechatWorkHttpClient.getDepartmentList();
        if (isApiError(departmentResult)) {
            return TaskResult.fail(0, 1, "獲取企業微信部門失敗: " + departmentResult.getString("errmsg"));
        }
        // 同步部門列表
        wecomSchoolDepartmentService.syncWecomSchoolDepartments(departmentResult);

        // 2. 獲取部門列表的成員並同步寫入
        int memberFailCount = 0;
        String firstMemberErrorReason = null;
        
        if (departmentResult != null && !isApiError(departmentResult)) {

            JSONArray departmentArray = departmentResult.getJSONArray("department");
            // 遍歷部門列表
            if (departmentArray != null && !departmentArray.isEmpty()) {
                for (int i = 0; i < departmentArray.size(); i++) {
                    JSONObject deptObj = departmentArray.getJSONObject(i);
                    Long deptId = deptObj.getLong("id");
                    // 獲取部門成員列表api
                    JSONObject memberResult = wechatWorkHttpClient.getDepartmentMembers(deptId);
                    
                    if (isApiError(memberResult)) {
                        memberFailCount++;
                        if (firstMemberErrorReason == null) {
                            firstMemberErrorReason = memberResult.getString("errmsg");
                        }
                    }
                    
                    // 同步部門成員
                    wecomSchoolDepartmentService.syncWecomSchoolDepartmentMembers(deptId, memberResult);
                }
            }
        }
        
        int totalDepts = departmentResult != null && departmentResult.getJSONArray("department") != null 
                ? departmentResult.getJSONArray("department").size() : 0;
                
        if (memberFailCount > 0) {
            return new TaskResult(totalDepts - memberFailCount, memberFailCount, "共 " + memberFailCount + " 個部門成員同步失敗，原因: " + firstMemberErrorReason);
        }

        log.info("企業微信部門與成員數據同步完成");
        return TaskResult.success(totalDepts, 0, "同步成功");
    }
}
