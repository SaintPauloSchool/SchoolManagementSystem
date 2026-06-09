package com.sms.handler.system;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.framework.wechat.WechatWorkHttpClient;
import com.sms.system.entity.SysStudentMatch;
import com.sms.system.service.ISysStudentMatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 學生姓名同步處理器
 * 負責調用企業微信 batch_update_student API，並協調 Service 更新本地資料庫
 * 放在 sms-api 以便直接注入 WechatWorkHttpClient（避免 sms-system 與 sms-framework 循環依賴）
 */
@Component
public class StudentMatchHandler {

    private static final Logger log = LoggerFactory.getLogger(StudentMatchHandler.class);

    /** 每批次最多同步 100 筆（企微接口限制） */
    private static final int BATCH_LIMIT = 100;

    @Autowired
    private WechatWorkHttpClient wechatWorkHttpClient;

    @Autowired
    private ISysStudentMatchService sysStudentMatchService;

    /**
     * 批量將已匹配學生姓名同步至企業微信，並回寫本地資料庫
     *
     * @param matchIds  要同步的匹配記錄 ID 列表
     * @param operName  操作人名稱（寫入 update_by）
     * @return 包含 successCount / failCount / message 的結果 Map
     */
    public Map<String, Object> syncStudentNames(List<Long> matchIds, String operName) {
        Map<String, Object> resultMap = new HashMap<>();

        if (matchIds == null || matchIds.isEmpty()) {
            resultMap.put("successCount", 0);
            resultMap.put("failCount", 0);
            resultMap.put("message", "請選擇要同步的匹配記錄！");
            return resultMap;
        }

        // 1. 從 DB 取出待同步記錄
        List<SysStudentMatch> pendingList = sysStudentMatchService.getPendingListForSync(matchIds);
        
        // 過濾掉未匹配 (studentUserIdWecom 為空) 的記錄
        List<SysStudentMatch> validList = new ArrayList<>();
        for (SysStudentMatch m : pendingList) {
            if (m.getStudentUserIdWecom() != null && !m.getStudentUserIdWecom().trim().isEmpty()) {
                validList.add(m);
            }
        }

        if (validList.isEmpty()) {
            resultMap.put("successCount", 0);
            resultMap.put("failCount", 0);
            resultMap.put("message", "選中的記錄中沒有符合同步條件的項目（必須已匹配且未同步成功）！");
            return resultMap;
        }

        // 2. 批量查詢學生的企微班級部門 ID（用於 payload 保護，防止更名覆蓋班級）
        List<String> studentUserIds = new ArrayList<>();
        for (SysStudentMatch m : validList) {
            studentUserIds.add(m.getStudentUserIdWecom());
        }
        Map<String, List<Long>> studentDeptsMap = sysStudentMatchService.getStudentDeptMap(studentUserIds);

        int totalSuccess = 0;
        int totalFail = 0;
        StringBuilder errorSummary = new StringBuilder();

        // 3. 分批（每批 100 筆）調用企微接口
        for (int i = 0; i < validList.size(); i += BATCH_LIMIT) {
            List<SysStudentMatch> subList = validList.subList(i, Math.min(i + BATCH_LIMIT, validList.size()));

            // 構建企微請求 payload
            JSONArray studentsArray = new JSONArray();
            for (SysStudentMatch matchItem : subList) {
                JSONObject stuObj = new JSONObject();
                stuObj.put("student_userid", matchItem.getStudentUserIdWecom());
                stuObj.put("name", matchItem.getStudentNameLocal());

                // 攜帶原有班級 ID，防止更名操作意外覆蓋並踢出學生的班級
                JSONArray depts = new JSONArray();
                List<Long> deptIds = studentDeptsMap.get(matchItem.getStudentUserIdWecom());
                if (deptIds != null) {
                    for (Long dId : deptIds) {
                        depts.add(dId);
                    }
                }
                stuObj.put("department", depts);

                studentsArray.add(stuObj);
            }

            try {
                JSONObject wecomResponse = wechatWorkHttpClient.batchUpdateStudent(studentsArray);

                if (wecomResponse != null && wecomResponse.getInteger("errcode") == 0) {
                    // 解析個別學生的更新結果
                    JSONArray resultList = wecomResponse.getJSONArray("result_list");
                    Map<String, JSONObject> statusMap = new HashMap<>();
                    if (resultList != null) {
                        for (int k = 0; k < resultList.size(); k++) {
                            JSONObject rObj = resultList.getJSONObject(k);
                            statusMap.put(rObj.getString("student_userid"), rObj);
                        }
                    }

                    for (SysStudentMatch matchItem : subList) {
                        JSONObject rObj = statusMap.get(matchItem.getStudentUserIdWecom());
                        if (rObj != null && rObj.getInteger("errcode") == 0) {
                            sysStudentMatchService.saveOneSyncResult(matchItem, "1", null, operName);
                            totalSuccess++;
                        } else {
                            String errmsg = rObj != null ? rObj.getString("errmsg") : "企業微信同步無回傳狀態";
                            sysStudentMatchService.saveOneSyncResult(matchItem, "2", errmsg, operName);
                            totalFail++;
                            errorSummary.append("<br/>").append(matchItem.getStudentNameLocal()).append("：").append(errmsg);
                        }
                    }
                } else {
                    // 整批 API 調用失敗
                    String globalErrMsg = wecomResponse != null ? wecomResponse.getString("errmsg") : "企業微信接口未回傳有效內容";
                    for (SysStudentMatch matchItem : subList) {
                        sysStudentMatchService.saveOneSyncResult(matchItem, "2", globalErrMsg, operName);
                        totalFail++;
                    }
                    errorSummary.append("<br/>分批 API 調用失敗：").append(globalErrMsg);
                }
            } catch (Exception ex) {
                log.error("調用企微更名接口拋出異常", ex);
                for (SysStudentMatch matchItem : subList) {
                    sysStudentMatchService.saveOneSyncResult(matchItem, "2", "系統錯誤：" + ex.getMessage(), operName);
                    totalFail++;
                }
                errorSummary.append("<br/>分批執行異常：").append(ex.getMessage());
            }
        }

        resultMap.put("successCount", totalSuccess);
        resultMap.put("failCount", totalFail);
        String msg = String.format("同步處理完成！成功 %d 筆，失敗 %d 筆。", totalSuccess, totalFail);
        if (totalFail > 0) {
            msg += " 錯誤詳情：" + errorSummary;
        }
        resultMap.put("message", msg);
        return resultMap;
    }
}
