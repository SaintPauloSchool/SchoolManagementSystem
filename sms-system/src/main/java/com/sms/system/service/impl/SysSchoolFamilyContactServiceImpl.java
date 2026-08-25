package com.sms.system.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.SysSchoolFamilyContact;
import com.sms.system.mapper.SysSchoolFamilyContactMapper;
import com.sms.system.service.ISysConfigService;
import com.sms.system.service.ISysSchoolFamilyContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SysSchoolFamilyContactServiceImpl implements ISysSchoolFamilyContactService {

    private static final Logger logger = LoggerFactory.getLogger(SysSchoolFamilyContactServiceImpl.class);

    @Autowired
    private SysSchoolFamilyContactMapper schoolFamilyContactMapper;

    @Autowired
    private ISysConfigService sysConfigService;

    @Override
    public List<String> getAllParentUserIds() {
        List<Long> classDepartmentIds = sysConfigService.getDailyNoticeClassDepartmentIds();
        if (classDepartmentIds == null || classDepartmentIds.isEmpty()) {
            logger.warn("未配置每日學生手冊通知班級範圍，跳過查詢家長列表");
            return Collections.emptyList();
        }

        List<String> parentUserIds = schoolFamilyContactMapper
                .selectParentUserIdsByDepartmentIds(classDepartmentIds);
        return parentUserIds != null ? parentUserIds : Collections.emptyList();
    }

    /**
     * 同步指定班級部門的企微家校聯絡人。
     * <p>
     * 按 {@code parent_user_id + student_user_id} 全局查找已有記錄：
     * 升班時更新 {@code department_id}（搬家），不新增重複行；
     * 若同一家長+學生已有多條，保留一條並刪除其餘。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncSchoolFamilyContactData(Long targetDepartmentId, JSONObject parentJson) {
        if (parentJson == null || parentJson.getInteger("errcode") == null
                || parentJson.getInteger("errcode") != 0) {
            logger.error("獲取部門 ID {} 的家長列表失敗：{}", targetDepartmentId,
                    parentJson != null ? parentJson.getString("errmsg") : "返回結果為空");
            return;
        }

        JSONArray parentsArray = parentJson.getJSONArray("parents");
        if (parentsArray == null || parentsArray.isEmpty()) {
            logger.info("部門 ID {} 的家長列表為空，保留本地數據", targetDepartmentId);
            return;
        }

        logger.info("部門 ID {} 成功獲取到 {} 個家長資訊", targetDepartmentId, parentsArray.size());

        // 先解析企微返回，收集本班應有的聯絡人
        List<SysSchoolFamilyContact> wecomContacts = new ArrayList<>();
        Set<String> currentKeys = new HashSet<>();
        Set<String> parentUserIds = new HashSet<>();

        for (int i = 0; i < parentsArray.size(); i++) {
            JSONObject parentObj = parentsArray.getJSONObject(i);
            String parentUserId = parentObj.getString("parent_userid");
            String mobile = parentObj.getString("mobile");
            String externalUserid = parentObj.getString("external_userid");
            if (parentUserId == null) {
                continue;
            }

            JSONArray childrenArray = parentObj.getJSONArray("children");
            if (childrenArray == null || childrenArray.isEmpty()) {
                continue;
            }

            for (int j = 0; j < childrenArray.size(); j++) {
                JSONObject childObj = childrenArray.getJSONObject(j);
                String studentUserId = childObj.getString("student_userid");
                if (studentUserId == null) {
                    continue;
                }

                String key = buildContactKey(parentUserId, studentUserId);
                currentKeys.add(key);
                parentUserIds.add(parentUserId);

                wecomContacts.add(buildContactEntity(
                        targetDepartmentId,
                        parentUserId,
                        studentUserId,
                        childObj.getString("name"),
                        childObj.getString("relation"),
                        mobile,
                        externalUserid
                ));
            }
        }

        if (wecomContacts.isEmpty()) {
            logger.info("部門 ID {} 解析後無有效家長-學生聯絡人", targetDepartmentId);
            return;
        }

        // 全局按家長查已有記錄（含其他班級），用於升班搬家與去重
        Map<String, List<SysSchoolFamilyContact>> existingByKey = loadExistingContactsByParents(parentUserIds);

        List<SysSchoolFamilyContact> toInsert = new ArrayList<>();
        List<SysSchoolFamilyContact> toUpdate = new ArrayList<>();
        List<Long> duplicateIdsToDelete = new ArrayList<>();
        Set<String> pendingInsertKeys = new HashSet<>();

        for (SysSchoolFamilyContact wecomContact : wecomContacts) {
            String key = buildContactKey(wecomContact.getParentUserId(), wecomContact.getStudentUserId());
            if (pendingInsertKeys.contains(key)) {
                continue;
            }

            List<SysSchoolFamilyContact> existingList = existingByKey.getOrDefault(key, Collections.emptyList());
            if (existingList.isEmpty()) {
                toInsert.add(wecomContact);
                pendingInsertKeys.add(key);
                continue;
            }

            SysSchoolFamilyContact keep = pickContactToKeep(existingList, targetDepartmentId);
            for (SysSchoolFamilyContact extra : existingList) {
                if (extra.getId() != null && !Objects.equals(extra.getId(), keep.getId())) {
                    duplicateIdsToDelete.add(extra.getId());
                }
            }

            if (needsUpdate(keep, wecomContact) || !Objects.equals(keep.getDepartmentId(), targetDepartmentId)) {
                Long oldDeptId = keep.getDepartmentId();
                keep.setDepartmentId(targetDepartmentId);
                keep.setStudentName(wecomContact.getStudentName());
                keep.setRelationDesc(wecomContact.getRelationDesc());
                keep.setMobile(wecomContact.getMobile());
                keep.setExternalUserid(wecomContact.getExternalUserid());
                keep.setUpdateTime(LocalDateTime.now());
                toUpdate.add(keep);
                if (!Objects.equals(oldDeptId, targetDepartmentId)) {
                    logger.info("聯絡人升班搬家: parent={}, student={}, {} → {}",
                            keep.getParentUserId(), keep.getStudentUserId(), oldDeptId, targetDepartmentId);
                }
            }

            existingByKey.put(key, Collections.singletonList(keep));
        }

        if (!toInsert.isEmpty()) {
            int batchSize = 1000;
            for (int i = 0; i < toInsert.size(); i += batchSize) {
                List<SysSchoolFamilyContact> subList = toInsert.subList(i, Math.min(i + batchSize, toInsert.size()));
                schoolFamilyContactMapper.batchInsert(subList);
            }
            logger.info("部門 ID {} 批量新增了 {} 條家校通訊錄聯絡人", targetDepartmentId, toInsert.size());
        }

        if (!toUpdate.isEmpty()) {
            for (SysSchoolFamilyContact contact : toUpdate) {
                schoolFamilyContactMapper.updateContact(contact);
            }
            logger.info("部門 ID {} 更新了 {} 條家校通訊錄聯絡人（含升班搬家）", targetDepartmentId, toUpdate.size());
        }

        if (!duplicateIdsToDelete.isEmpty()) {
            List<Long> distinctIds = duplicateIdsToDelete.stream().distinct().collect(Collectors.toList());
            schoolFamilyContactMapper.deleteBatchByIds(distinctIds);
            logger.info("部門 ID {} 同步時清理重複聯絡人 {} 條", targetDepartmentId, distinctIds.size());
        }

        // 清理「仍掛在本班、但本次企微未返回」的過期記錄
        List<SysSchoolFamilyContact> contactsInTargetDept =
                schoolFamilyContactMapper.selectByDepartmentId(targetDepartmentId);
        deleteObsoleteContactsForDepartment(targetDepartmentId, contactsInTargetDept, currentKeys);
    }

    /**
     * 按家長列表加載全部已有聯絡人，並按 parent+student 分組。
     */
    private Map<String, List<SysSchoolFamilyContact>> loadExistingContactsByParents(Set<String> parentUserIds) {
        Map<String, List<SysSchoolFamilyContact>> map = new HashMap<>();
        if (parentUserIds == null || parentUserIds.isEmpty()) {
            return map;
        }
        List<SysSchoolFamilyContact> existing =
                schoolFamilyContactMapper.selectByParentUserIds(new ArrayList<>(parentUserIds));
        if (existing == null) {
            return map;
        }
        for (SysSchoolFamilyContact contact : existing) {
            if (contact.getParentUserId() == null || contact.getStudentUserId() == null) {
                continue;
            }
            String key = buildContactKey(contact.getParentUserId(), contact.getStudentUserId());
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(contact);
        }
        return map;
    }

    /**
     * 同一家長+學生多條時：優先保留已在目標班級的；否則保留 id 最大（最新）的一條。
     */
    private SysSchoolFamilyContact pickContactToKeep(List<SysSchoolFamilyContact> existingList,
                                                     Long targetDepartmentId) {
        return existingList.stream()
                .filter(c -> c.getId() != null)
                .max(Comparator
                        .comparing((SysSchoolFamilyContact c) ->
                                Objects.equals(c.getDepartmentId(), targetDepartmentId))
                        .thenComparing(SysSchoolFamilyContact::getId))
                .orElse(existingList.get(0));
    }

    /**
     * 刪除指定部門中，本次企微同步未返回的過期聯絡人。
     */
    private void deleteObsoleteContactsForDepartment(Long departmentId,
                                                     List<SysSchoolFamilyContact> existingContacts,
                                                     Set<String> currentKeys) {
        if (existingContacts == null || existingContacts.isEmpty()) {
            return;
        }

        List<Long> toDeleteIds = new ArrayList<>();
        for (SysSchoolFamilyContact contact : existingContacts) {
            String key = buildContactKey(contact.getParentUserId(), contact.getStudentUserId());
            if (!currentKeys.contains(key)) {
                toDeleteIds.add(contact.getId());
            }
        }

        if (!toDeleteIds.isEmpty()) {
            schoolFamilyContactMapper.deleteBatchByIds(toDeleteIds);
            logger.info("部門 ID {} 批量刪除了 {} 條過期的家校通訊錄聯絡人", departmentId, toDeleteIds.size());
        }
    }

    private SysSchoolFamilyContact buildContactEntity(Long departmentId,
                                                      String parentUserId,
                                                      String studentUserId,
                                                      String studentName,
                                                      String relation,
                                                      String mobile,
                                                      String externalUserid) {
        SysSchoolFamilyContact contact = new SysSchoolFamilyContact();
        contact.setDepartmentId(departmentId);
        contact.setParentUserId(parentUserId);
        contact.setStudentUserId(studentUserId);
        contact.setStudentName(studentName);
        contact.setRelationDesc(relation);
        contact.setMobile(mobile);
        contact.setExternalUserid(externalUserid);
        contact.setCreateTime(LocalDateTime.now());
        contact.setUpdateTime(LocalDateTime.now());
        return contact;
    }

    private String buildContactKey(String parentUserId, String studentUserId) {
        return parentUserId + "_" + studentUserId;
    }

    /**
     * 判斷本地聯絡人是否需要根據企微最新資料更新（不含 department_id，班級另判）。
     */
    private boolean needsUpdate(SysSchoolFamilyContact dbContact, SysSchoolFamilyContact wecomContact) {
        return isDifferent(dbContact.getStudentName(), wecomContact.getStudentName())
                || isDifferent(dbContact.getRelationDesc(), wecomContact.getRelationDesc())
                || isDifferent(dbContact.getMobile(), wecomContact.getMobile())
                || isDifferent(dbContact.getExternalUserid(), wecomContact.getExternalUserid());
    }

    private boolean isDifferent(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return false;
        }
        if (s1 == null || s2 == null) {
            return true;
        }
        return !s1.equals(s2);
    }
}
