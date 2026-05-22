package com.sms.handler.wecom;

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
     * 同步家校通訊錄部門與管理員數據
     * 由 DepartmentSyncTask 調用（每日凌晨 0 點）
     */
    public void syncSchoolDepartments() {
        log.info("開始同步家校通訊錄部門數據");

        JSONObject departmentJson = wechatWorkHttpClient.getSchoolDepartmentList();
        departmentService.syncSchoolDepartmentData(departmentJson);

        log.info("家校通訊錄部門數據同步完成");
    }

    /**
     * 同步家長學生關係數據
     * 由 ParentStudentRelationSyncTask 調用（每日凌晨 0 點 30 分）
     */
    public void syncParentStudentRelations() {
        log.info("開始同步家長學生關係數據");
        // 獲取所有班級部門 ID
        List<Long> targetDepartmentIds = departmentService.getClassDepartmentId();
        if (targetDepartmentIds == null || targetDepartmentIds.isEmpty()) {
            log.error("無法獲取目標班級部門 ID，同步任務終止");
            return;
        }

        log.info("成功獲取到 {} 個目標部門 ID", targetDepartmentIds.size());
        // 遍歷所有班級部門 ID
        for (Long targetDepartmentId : targetDepartmentIds) {
            log.info("開始執行部門 ID {} 的家長學生關係同步", targetDepartmentId);
            // 獲取企微家校家長列表api
            JSONObject parentJson = wechatWorkHttpClient.getSchoolParentList(targetDepartmentId);
            parentStudentRelationService.syncParentStudentRelationData(targetDepartmentId, parentJson);
        }

        log.info("家長學生關係數據同步完成");
    }

    /**
     * 同步企業微信部門與成員數據
     * 由 WecomSchoolDepartmentTask 調用（每日凌晨 1 點）
     */
    public void syncWecomDepartmentsAndMembers() {
        log.info("開始同步企業微信部門與成員數據");

        // 1. 獲取並同步部門列表
        JSONObject departmentResult = wechatWorkHttpClient.getDepartmentList();
        // 同步部門列表
        wecomSchoolDepartmentService.syncWecomSchoolDepartments(departmentResult);

        // 2. 根據部門列表逐一獲取並同步成員
        if (departmentResult != null
                && departmentResult.getInteger("errcode") != null
                && departmentResult.getInteger("errcode") == 0) {

            JSONArray departmentArray = departmentResult.getJSONArray("department");
            // 遍歷部門列表
            if (departmentArray != null && !departmentArray.isEmpty()) {
                for (int i = 0; i < departmentArray.size(); i++) {
                    JSONObject deptObj = departmentArray.getJSONObject(i);
                    Long deptId = deptObj.getLong("id");
                    // 獲取部門成員列表api
                    JSONObject memberResult = wechatWorkHttpClient.getDepartmentMembers(deptId);
                    // 同步部門成員
                    wecomSchoolDepartmentService.syncWecomSchoolDepartmentMembers(deptId, memberResult);
                }
            }
        }

        log.info("企業微信部門與成員數據同步完成");
    }
}
