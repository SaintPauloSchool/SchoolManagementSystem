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
     * 由 WecomSyncHandler 在遍歷班級時調用，僅同步 department_parent_binding，並收集並返回該班級的家長關係對象列表。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SysParentStudentRelation> syncParentStudentRelationData(Long targetDepartmentId, JSONObject parentJson) {
        List<SysParentStudentRelation> relations = new ArrayList<>();
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
                Set<String> currentParentUserIds = new HashSet<>();

                for (int i = 0; i < parentsArray.size(); i++) {
                    JSONObject parentObj = parentsArray.getJSONObject(i);
                    String parentUserId = parentObj.getString("parent_userid");
                    String mobile = parentObj.getString("mobile");
                    String externalUserid = parentObj.getString("external_userid");
                    if (parentUserId == null) continue;

                    currentParentUserIds.add(parentUserId);
                    JSONArray childrenArray = parentObj.getJSONArray("children");

                    // 處理 departmentParentBinding
                    departmentParentBindingService.processParentChildren(targetDepartmentId, parentUserId, childrenArray, existingBindingMap);

                    // 收集該家長的孩子關係，但不立即進行資料庫增刪改，留待全局同步時處理
                    if (childrenArray != null && !childrenArray.isEmpty()) {
                        for (int j = 0; j < childrenArray.size(); j++) {
                            JSONObject childObj = childrenArray.getJSONObject(j);
                            String studentUserId = childObj.getString("student_userid");
                            String relationDesc = childObj.getString("relation");
                            String studentName = childObj.getString("name");
                            if (studentUserId == null) continue;

                            SysParentStudentRelation rel = buildRelationEntity(parentUserId, studentUserId, studentName, relationDesc, mobile, externalUserid);
                            relations.add(rel);
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
        return relations;
    }

    /**
     * 全局批量同步與比對家長學生關係
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncAllParentStudentRelations(List<SysParentStudentRelation> wecomRelations, boolean shouldDeleteObsolete) {
        if (wecomRelations == null) {
            return;
        }

        logger.info("開始全局同步家長學生關係，共收到企微關係資料 {} 條，是否執行刪除清理：{}", wecomRelations.size(), shouldDeleteObsolete);

        // 1. 查詢本地數據庫中的所有關係記錄
        List<SysParentStudentRelation> dbRelations = sysParentStudentRelationMapper.selectAllRelations();

        // 建立本地關係的 Map，key 為 parent_user_id + "_" + student_user_id
        Map<String, SysParentStudentRelation> dbRelationsMap = new HashMap<>();
        if (dbRelations != null) {
            for (SysParentStudentRelation rel : dbRelations) {
                String key = rel.getParentUserId() + "_" + rel.getStudentUserId();
                dbRelationsMap.put(key, rel);
            }
        }

        List<SysParentStudentRelation> toInsert = new ArrayList<>();
        List<SysParentStudentRelation> toUpdate = new ArrayList<>();

        // 記錄在 WeCom 中出現的關係 key，用於後續找出需要刪除的記錄
        Set<String> wecomKeys = new HashSet<>();

        // 2. 比對 WeCom 數據
        for (SysParentStudentRelation wecomRel : wecomRelations) {
            String key = wecomRel.getParentUserId() + "_" + wecomRel.getStudentUserId();
            wecomKeys.add(key);

            SysParentStudentRelation dbRel = dbRelationsMap.get(key);
            if (dbRel == null) {
                // 資料庫不存在 -> 新增
                toInsert.add(wecomRel);
            } else {
                // 資料庫存在 -> 檢查更新
                boolean needUpdate = false;
                if (isDifferent(dbRel.getStudentName(), wecomRel.getStudentName())) {
                    dbRel.setStudentName(wecomRel.getStudentName());
                    needUpdate = true;
                }
                if (isDifferent(dbRel.getRelationDesc(), wecomRel.getRelationDesc())) {
                    dbRel.setRelationDesc(wecomRel.getRelationDesc());
                    needUpdate = true;
                }
                if (isDifferent(dbRel.getMobile(), wecomRel.getMobile())) {
                    dbRel.setMobile(wecomRel.getMobile());
                    needUpdate = true;
                }
                if (isDifferent(dbRel.getExternalUserid(), wecomRel.getExternalUserid())) {
                    dbRel.setExternalUserid(wecomRel.getExternalUserid());
                    needUpdate = true;
                }
                if (needUpdate) {
                    dbRel.setUpdateTime(LocalDateTime.now());
                    toUpdate.add(dbRel);
                }
            }
        }

        // 3. 找出需要刪除的關係
        List<SysParentStudentRelation> toDelete = new ArrayList<>();
        if (shouldDeleteObsolete && dbRelations != null && !wecomRelations.isEmpty()) {
            for (SysParentStudentRelation dbRel : dbRelations) {
                String key = dbRel.getParentUserId() + "_" + dbRel.getStudentUserId();
                if (!wecomKeys.contains(key)) {
                    toDelete.add(dbRel);
                }
            }
        }

        // 4. 執行批次操作
        if (!toInsert.isEmpty()) {
            int batchSize = 1000;
            for (int i = 0; i < toInsert.size(); i += batchSize) {
                List<SysParentStudentRelation> subList = toInsert.subList(i, Math.min(i + batchSize, toInsert.size()));
                sysParentStudentRelationMapper.batchInsert(subList);
            }
            logger.info("全局同步：批量新增了 {} 條家長學生關係", toInsert.size());
        }

        if (!toUpdate.isEmpty()) {
            for (SysParentStudentRelation rel : toUpdate) {
                sysParentStudentRelationMapper.updateRelation(rel);
            }
            logger.info("全局同步：更新了 {} 條家長學生關係", toUpdate.size());
        }

        if (shouldDeleteObsolete && !toDelete.isEmpty()) {
            int batchSize = 1000;
            for (int i = 0; i < toDelete.size(); i += batchSize) {
                List<SysParentStudentRelation> subList = toDelete.subList(i, Math.min(i + batchSize, toDelete.size()));
                sysParentStudentRelationMapper.deleteBatch(subList);
            }
            logger.info("全局同步：批量刪除了 {} 條過期的家長學生關係", toDelete.size());
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
