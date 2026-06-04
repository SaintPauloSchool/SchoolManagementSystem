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
import java.util.ArrayList;
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
     * @p    /**
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
        SysParentStudentRelation relationEntity = buildRelationEntity(parentUserId, studentUserId, studentName, relation, mobile, externalUserid);
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

                // 收集當前同步的家長 ID 列表
                List<String> currentParentUserIds = new ArrayList<>();
                // 建立 parentUserId -> childrenArray 的 Map
                Map<String, JSONArray> parentChildrenMap = new HashMap<>();
                // 建立 parentUserId -> parentObj 的 Map
                Map<String, JSONObject> parentInfoMap = new HashMap<>();

                for (int i = 0; i < parentsArray.size(); i++) {
                    JSONObject parentObj = parentsArray.getJSONObject(i);
                    String parentUserId = parentObj.getString("parent_userid");
                    if (parentUserId != null) {
                        currentParentUserIds.add(parentUserId);
                        parentChildrenMap.put(parentUserId, parentObj.getJSONArray("children"));
                        parentInfoMap.put(parentUserId, parentObj);
                    }
                }

                // 1. 批量查詢這些家長在本地數據庫中的所有關係記錄
                List<SysParentStudentRelation> dbRelations = new ArrayList<>();
                if (!currentParentUserIds.isEmpty()) {
                    dbRelations = sysParentStudentRelationMapper.selectByParentUserIds(currentParentUserIds);
                }

                // 按 parent_user_id 分組
                Map<String, List<SysParentStudentRelation>> dbRelationsMap = new HashMap<>();
                if (dbRelations != null) {
                    for (SysParentStudentRelation rel : dbRelations) {
                        dbRelationsMap.computeIfAbsent(rel.getParentUserId(), k -> new ArrayList<>()).add(rel);
                    }
                }

                // 用於批量操作的集合
                List<SysParentStudentRelation> toInsert = new ArrayList<>();
                List<SysParentStudentRelation> toUpdate = new ArrayList<>();
                List<SysParentStudentRelation> toDelete = new ArrayList<>();

                // 2. 遍歷當前同步的家長
                for (String parentUserId : currentParentUserIds) {
                    JSONObject parentObj = parentInfoMap.get(parentUserId);
                    String mobile = parentObj.getString("mobile");
                    String externalUserid = parentObj.getString("external_userid");
                    JSONArray childrenArray = parentChildrenMap.get(parentUserId);

                    // 處理 departmentParentBinding
                    departmentParentBindingService.processParentChildren(targetDepartmentId, parentUserId, childrenArray, existingBindingMap);

                    // 本地庫中該家長已有的關係
                    List<SysParentStudentRelation> dbUserRelations = dbRelationsMap.getOrDefault(parentUserId, new ArrayList<>());
                    Map<String, SysParentStudentRelation> dbUserRelationsByStudent = new HashMap<>();
                    for (SysParentStudentRelation rel : dbUserRelations) {
                        dbUserRelationsByStudent.put(rel.getStudentUserId(), rel);
                    }

                    // WeChat Work 返回 of 該家長的孩子關係
                    Set<String> wecomStudentIds = new HashSet<>();
                    if (childrenArray != null && !childrenArray.isEmpty()) {
                        for (int j = 0; j < childrenArray.size(); j++) {
                            JSONObject childObj = childrenArray.getJSONObject(j);
                            String studentUserId = childObj.getString("student_userid");
                            String relationDesc = childObj.getString("relation");
                            String studentName = childObj.getString("name");
                            if (studentUserId == null) continue;
                            wecomStudentIds.add(studentUserId);

                            SysParentStudentRelation dbRel = dbUserRelationsByStudent.get(studentUserId);
                            if (dbRel == null) {
                                // 數據庫中不存在此關係 -> 新增 (不用 insertIgnore 避免空耗自增 ID)
                                SysParentStudentRelation newRel = buildRelationEntity(parentUserId, studentUserId, studentName, relationDesc, mobile, externalUserid);
                                toInsert.add(newRel);
                            } else {
                                // 數據庫中已存在 -> 檢查是否需要更新
                                boolean needUpdate = false;
                                if (isDifferent(dbRel.getStudentName(), studentName)) {
                                    dbRel.setStudentName(studentName);
                                    needUpdate = true;
                                }
                                if (isDifferent(dbRel.getRelationDesc(), relationDesc)) {
                                    dbRel.setRelationDesc(relationDesc);
                                    needUpdate = true;
                                }
                                if (isDifferent(dbRel.getMobile(), mobile)) {
                                    dbRel.setMobile(mobile);
                                    needUpdate = true;
                                }
                                if (isDifferent(dbRel.getExternalUserid(), externalUserid)) {
                                    dbRel.setExternalUserid(externalUserid);
                                    needUpdate = true;
                                }
                                if (needUpdate) {
                                    dbRel.setUpdateTime(LocalDateTime.now());
                                    toUpdate.add(dbRel);
                                }
                            }
                        }
                    }

                    // 找出本地數據庫有，但 WeCom 中已被刪除の關係 -> 刪除
                    for (SysParentStudentRelation dbRel : dbUserRelations) {
                        if (!wecomStudentIds.contains(dbRel.getStudentUserId())) {
                            toDelete.add(dbRel);
                        }
                    }
                }

                // 3. 執行批次與更新操作
                if (!toInsert.isEmpty()) {
                    sysParentStudentRelationMapper.batchInsert(toInsert);
                    logger.info("部門 ID {} 同步：批量新增了 {} 條家長學生關係", targetDepartmentId, toInsert.size());
                }
                if (!toUpdate.isEmpty()) {
                    for (SysParentStudentRelation rel : toUpdate) {
                        sysParentStudentRelationMapper.updateRelation(rel);
                    }
                    logger.info("部門 ID {} 同步：更新了 {} 條家長學生關係", targetDepartmentId, toUpdate.size());
                }
                if (!toDelete.isEmpty()) {
                    sysParentStudentRelationMapper.deleteBatch(toDelete);
                    logger.info("部門 ID {} 同步：批量刪除了 {} 條過期的家長學生關係", targetDepartmentId, toDelete.size());
                }

                // 委託給 Service 層刪除不再存在的家長綁定記錄
                departmentParentBindingService.deleteObsoleteParentBindings(existingBindings, new HashSet<>(currentParentUserIds), targetDepartmentId);
            } else {
                logger.info("部門 ID {} 的家長列表為空", targetDepartmentId);
            }
        } else {
            logger.error("獲取部門 ID {} 的家長列表失敗：{}", targetDepartmentId, parentJson != null ? parentJson.getString("errmsg") : "返回結果為空");
        }
    }

    /**
     * 全局清理已不在任何部門綁定中的孤立家長學生關係記錄
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrphanRelations() {
        int deletedCount = sysParentStudentRelationMapper.deleteOrphanRelations();
        if (deletedCount > 0) {
            logger.info("全局清理：已成功清理 {} 條已無效的孤立家長學生關係記錄 (在部門綁定中無對應記錄)", deletedCount);
        }
    }

    /**
     * 構建家長學生關係實體對象的輔助方法
     */
    private SysParentStudentRelation buildRelationEntity(String parentUserId, String studentUserId, String studentName,
                                                         String relation, String mobile, String externalUserid) {
        SysParentStudentRelation relationEntity = new SysParentStudentRelation();
        relationEntity.setParentUserId(parentUserId);
        relationEntity.setStudentUserId(studentUserId);
        relationEntity.setStudentName(studentName);
        relationEntity.setRelationDesc(relation);
        relationEntity.setMobile(mobile);
        relationEntity.setExternalUserid(externalUserid);
        relationEntity.setCreateTime(LocalDateTime.now());
        relationEntity.setUpdateTime(LocalDateTime.now());
        return relationEntity;
    }
    /**
     * 判斷兩個字串是否不同（考慮 null 情況）
     */
    private boolean isDifferent(String s1, String s2) {
        if (s1 == null && s2 == null) return false;
        if (s1 == null || s2 == null) return true;
        return !s1.equals(s2);
    }

}
