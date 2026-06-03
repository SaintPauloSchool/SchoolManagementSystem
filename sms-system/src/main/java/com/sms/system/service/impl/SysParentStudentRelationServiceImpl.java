package com.sms.system.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.entity.SysParentStudentRelation;
import com.sms.system.mapper.SysParentStudentRelationMapper;
import com.sms.system.service.ISysDepartmentParentBindingService;
import com.sms.system.service.ISysParentStudentRelationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 家長學生關係Service業務層處理
 */
@Service
public class SysParentStudentRelationServiceImpl implements ISysParentStudentRelationService {

    private static final Logger logger = LoggerFactory.getLogger(SysParentStudentRelationServiceImpl.class);

    @Autowired
    private SysParentStudentRelationMapper sysParentStudentRelationMapper;

    @Autowired
    private ISysDepartmentParentBindingService departmentParentBindingService;

    /**
     * 創建家長 - 孩子關係記錄
     *
     * @param parentUserId     家長用戶 ID
     * @param studentUserId    孩子用戶 ID
     * @param studentName      孩子姓名
     * @param relation         家長關係描述
     * @param mobile           家長手機號
     * @param externalUserid   家長外部 ID
     */
    @Override
    @Transactional
    public void createAndSaveParentStudentRelation(String parentUserId, String studentUserId, String studentName,
                                                   String relation, String mobile, String externalUserid) {
        // 創建家長學生關係實體
        SysParentStudentRelation relationEntity = new SysParentStudentRelation();
        relationEntity.setParentUserId(parentUserId);
        relationEntity.setStudentUserId(studentUserId);
        relationEntity.setStudentName(studentName);
        relationEntity.setRelationDesc(relation);
        relationEntity.setMobile(mobile);
        relationEntity.setExternalUserid(externalUserid);
        relationEntity.setCreateTime(LocalDateTime.now());
        relationEntity.setUpdateTime(LocalDateTime.now());
        // 插入
        int result = sysParentStudentRelationMapper.insertIgnore(relationEntity);
        if (result > 0) {
            logger.info("創建並保存家長學生關係記錄，家長: {}, 學生: {}", parentUserId, studentUserId);
        }
    }

    /**
     * 同步家長學生關係數據
     * 由 ParentStudentRelationSyncTask 調用（每日凌晨 0 點 30 分）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncParentStudentRelationData(Long targetDepartmentId, JSONObject parentJson) {
        // 不存在錯誤編碼則處裡數據
        if (parentJson != null && parentJson.getInteger("errcode") != null && parentJson.getInteger("errcode") == 0) {
            // 獲取家長數據
            JSONArray parentsArray = parentJson.getJSONArray("parents");
            // 家長數據存在，則處裡家長數據
            if (parentsArray != null && !parentsArray.isEmpty()) {
                logger.info("部門 ID {} 成功獲取到 {} 個家長信息", targetDepartmentId, parentsArray.size());
                // 獲取當前部門下所有已存在的家長綁定記錄
                List<SysDepartmentParentBinding> existingBindings = departmentParentBindingService.selectByDepartmentId(targetDepartmentId);
                // 創建現有記錄的映射，便於快速查找
                Map<String, SysDepartmentParentBinding> existingBindingMap = new HashMap<>();
                for (SysDepartmentParentBinding binding : existingBindings) {
                    existingBindingMap.put(binding.getParentUserId(), binding);
                }
                // 創建用於保存的 家長 - 孩子綁定記錄
                Set<String> currentParentUserIds = new HashSet<>();
                // 遍歷家長數據並保存到數據庫
                for (int i = 0; i < parentsArray.size(); i++) {
                    // 獲取當前家長數據
                    JSONObject parentObj = parentsArray.getJSONObject(i);
                    String parentUserId = parentObj.getString("parent_userid");
                    String mobile = parentObj.getString("mobile");
                    String externalUserid = parentObj.getString("external_userid");
                    // 記錄當前處理的家長 ID
                    currentParentUserIds.add(parentUserId);
                    // 處理孩子信息數組
                    JSONArray childrenArray = parentObj.getJSONArray("children");
                    // 委託給 Service 層處理家長學生關係同步
                    departmentParentBindingService.processParentChildren(targetDepartmentId, parentUserId, childrenArray, existingBindingMap);
                    // 創建並保存家長學生關係記錄（如果有孩子的話）
                    if (childrenArray != null && !childrenArray.isEmpty()) {
                        // 遍歷孩子信息
                        for (int j = 0; j < childrenArray.size(); j++) {
                            JSONObject childObj = childrenArray.getJSONObject(j);
                            String childStudentUserId = childObj.getString("student_userid");
                            String relation = childObj.getString("relation");
                            String name = childObj.getString("name");
                            // 委託給 Service 層創建家長 - 孩子關係記錄
                            createAndSaveParentStudentRelation(parentUserId, childStudentUserId, name, relation, mobile, externalUserid);
                        }
                    }
                }
                // 委託給 Service 層刪除不再存在的家長綁定記錄
                departmentParentBindingService.deleteObsoleteParentBindings(existingBindings, currentParentUserIds, targetDepartmentId);
            } else {
                logger.info("部門 ID {} 的家長列表為空", targetDepartmentId);
            }
        } else {
            logger.error("獲取部門 ID {} 的家長列表失敗：{}", targetDepartmentId, parentJson != null ? parentJson.getString("errmsg") : "返回結果為空");
        }
    }

}
