package com.sms.handler.system;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.framework.wechat.WechatWorkHttpClient;
import com.sms.system.entity.dto.SysStudentMatchDeptQueryDTO;
import com.sms.system.entity.dto.SysStudentMatchSyncDTO;
import com.sms.system.entity.dto.SysStudentMatchSyncRecordDTO;
import com.sms.system.entity.vo.SysStudentMatchDeptMapVO;
import com.sms.system.entity.vo.SysStudentMatchSyncResultVO;
import com.sms.system.entity.vo.SysStudentMatchVO;
import com.sms.system.service.ISysStudentMatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 學生姓名同步處理器
 * 負責調用企業微信 batch_update_student API，並協調 Service 更新本地資料庫
 */
@Component
public class StudentMatchHandler {

    private static final Logger log = LoggerFactory.getLogger(StudentMatchHandler.class);

    private static final int BATCH_LIMIT = 100;

    @Autowired
    private WechatWorkHttpClient wechatWorkHttpClient;

    @Autowired
    private ISysStudentMatchService sysStudentMatchService;

    public SysStudentMatchSyncResultVO syncStudentNames(SysStudentMatchSyncDTO studentMatchSyncDTO) {
        SysStudentMatchSyncResultVO resultVO = new SysStudentMatchSyncResultVO();

        if (studentMatchSyncDTO == null
                || studentMatchSyncDTO.getMatchIds() == null
                || studentMatchSyncDTO.getMatchIds().isEmpty()) {
            resultVO.setSuccessCount(0);
            resultVO.setFailCount(0);
            resultVO.setMessage("請選擇要同步的匹配記錄！");
            return resultVO;
        }

        String operName = studentMatchSyncDTO.getOperName();
        List<SysStudentMatchVO> pendingList = sysStudentMatchService.getPendingListForSync(studentMatchSyncDTO);

        List<SysStudentMatchVO> validList = new ArrayList<>();
        for (SysStudentMatchVO matchVO : pendingList) {
            if (matchVO.getStudentUserIdWecom() != null && !matchVO.getStudentUserIdWecom().trim().isEmpty()) {
                validList.add(matchVO);
            }
        }

        if (validList.isEmpty()) {
            resultVO.setSuccessCount(0);
            resultVO.setFailCount(0);
            resultVO.setMessage("選中的記錄中沒有符合同步條件的項目（必須已匹配且未同步成功）！");
            return resultVO;
        }

        SysStudentMatchDeptQueryDTO sysStudentMatchDeptQueryDTO = new SysStudentMatchDeptQueryDTO();
        List<String> studentUserIds = new ArrayList<>();
        for (SysStudentMatchVO matchVO : validList) {
            studentUserIds.add(matchVO.getStudentUserIdWecom());
        }
        sysStudentMatchDeptQueryDTO.setStudentUserIds(studentUserIds);
        SysStudentMatchDeptMapVO deptMapVO = sysStudentMatchService.getStudentDeptMap(sysStudentMatchDeptQueryDTO);
        Map<String, List<Long>> studentDeptsMap = deptMapVO.getStudentDeptMap();

        int totalSuccess = 0;
        int totalFail = 0;
        StringBuilder errorSummary = new StringBuilder();

        for (int i = 0; i < validList.size(); i += BATCH_LIMIT) {
            List<SysStudentMatchVO> subList = validList.subList(i, Math.min(i + BATCH_LIMIT, validList.size()));

            JSONArray studentsArray = new JSONArray();
            for (SysStudentMatchVO matchItem : subList) {
                JSONObject stuObj = new JSONObject();
                stuObj.put("student_userid", matchItem.getStudentUserIdWecom());
                stuObj.put("name", getSyncTargetName(matchItem));

                JSONArray depts = new JSONArray();
                List<Long> deptIds = studentDeptsMap.get(matchItem.getStudentUserIdWecom());
                if (deptIds != null) {
                    for (Long deptId : deptIds) {
                        depts.add(deptId);
                    }
                }
                stuObj.put("department", depts);

                studentsArray.add(stuObj);
            }

            try {
                JSONObject wecomResponse = wechatWorkHttpClient.batchUpdateStudent(studentsArray);

                if (wecomResponse != null && wecomResponse.getInteger("errcode") == 0) {
                    JSONArray resultList = wecomResponse.getJSONArray("result_list");
                    Map<String, JSONObject> statusMap = new HashMap<>();
                    if (resultList != null) {
                        for (int k = 0; k < resultList.size(); k++) {
                            JSONObject resultObj = resultList.getJSONObject(k);
                            statusMap.put(resultObj.getString("student_userid"), resultObj);
                        }
                    }

                    for (SysStudentMatchVO matchItem : subList) {
                        JSONObject resultObj = statusMap.get(matchItem.getStudentUserIdWecom());
                        if (resultObj != null && resultObj.getInteger("errcode") == 0) {
                            saveSyncResult(matchItem, "1", null, operName);
                            totalSuccess++;
                        } else {
                            String errmsg = resultObj != null ? resultObj.getString("errmsg") : "企業微信同步無回傳狀態";
                            saveSyncResult(matchItem, "2", errmsg, operName);
                            totalFail++;
                            errorSummary.append("<br/>")
                                    .append(getSyncTargetName(matchItem))
                                    .append("：")
                                    .append(errmsg);
                        }
                    }
                } else {
                    String globalErrMsg = wecomResponse != null ? wecomResponse.getString("errmsg") : "企業微信接口未回傳有效內容";
                    for (SysStudentMatchVO matchItem : subList) {
                        saveSyncResult(matchItem, "2", globalErrMsg, operName);
                        totalFail++;
                    }
                    errorSummary.append("<br/>分批 API 調用失敗：").append(globalErrMsg);
                }
            } catch (Exception ex) {
                log.error("調用企微更名接口拋出異常", ex);
                for (SysStudentMatchVO matchItem : subList) {
                    saveSyncResult(matchItem, "2", "系統錯誤：" + ex.getMessage(), operName);
                    totalFail++;
                }
                errorSummary.append("<br/>分批執行異常：").append(ex.getMessage());
            }
        }

        resultVO.setSuccessCount(totalSuccess);
        resultVO.setFailCount(totalFail);
        String message = String.format("同步處理完成！成功 %d 筆，失敗 %d 筆。", totalSuccess, totalFail);
        if (totalFail > 0) {
            message += " 錯誤詳情：" + errorSummary;
        }
        resultVO.setMessage(message);
        return resultVO;
    }

    private void saveSyncResult(SysStudentMatchVO matchVO, String syncStatus, String errorMsg, String operName) {
        SysStudentMatchSyncRecordDTO syncRecordDTO = new SysStudentMatchSyncRecordDTO();
        syncRecordDTO.setMatchId(matchVO.getId());
        syncRecordDTO.setStudentUserIdWecom(matchVO.getStudentUserIdWecom());
        syncRecordDTO.setSyncTargetName(getSyncTargetName(matchVO));
        syncRecordDTO.setSyncStatus(syncStatus);
        syncRecordDTO.setErrorMsg(errorMsg);
        syncRecordDTO.setOperName(operName);
        sysStudentMatchService.saveOneSyncResult(syncRecordDTO);
    }

    private String getSyncTargetName(SysStudentMatchVO matchVO) {
        if (matchVO == null || !StringUtils.hasText(matchVO.getIdName())) {
            return "";
        }
        return matchVO.getIdName().trim();
    }
}
