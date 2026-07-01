package com.sms.system.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.SysSchoolFamilyContact;
import com.sms.system.mapper.SysSchoolFamilyContactMapper;
import com.sms.system.service.ISysSchoolFamilyContactService;
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

@Service
public class SysSchoolFamilyContactServiceImpl implements ISysSchoolFamilyContactService {

    private static final Logger logger = LoggerFactory.getLogger(SysSchoolFamilyContactServiceImpl.class);

    @Autowired
    private SysSchoolFamilyContactMapper schoolFamilyContactMapper;

    @Override
    public List<String> getAllParentUserIds() {
        return schoolFamilyContactMapper.selectAllParentUserIds();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncSchoolFamilyContactData(Long targetDepartmentId, JSONObject parentJson) {
        // 驗證json是否有數據
        if (parentJson == null || parentJson.getInteger("errcode") == null
                || parentJson.getInteger("errcode") != 0) {
            logger.error("獲取部門 ID {} 的家長列表失敗：{}", targetDepartmentId,
                    parentJson != null ? parentJson.getString("errmsg") : "返回結果為空");
            return;
        }

        //獲取家長數據
        JSONArray parentsArray = parentJson.getJSONArray("parents");
        if (parentsArray == null || parentsArray.isEmpty()) {
            logger.info("部門 ID {} 的家長列表為空，保留本地數據", targetDepartmentId);
            return;
        }

        logger.info("部門 ID {} 成功獲取到 {} 個家長信息", targetDepartmentId, parentsArray.size());

        // 查詢指定班級部門下的全部聯絡人
        List<SysSchoolFamilyContact> existingContacts =
                schoolFamilyContactMapper.selectByDepartmentId(targetDepartmentId);

        // 建立本地已有聯絡人索引，key = parentUserId_studentUserId，便於比對
        Map<String, SysSchoolFamilyContact> existingMap = new HashMap<>();
        if (existingContacts != null) {
            for (SysSchoolFamilyContact contact : existingContacts) {
                existingMap.put(buildContactKey(contact.getParentUserId(), contact.getStudentUserId()), contact);
            }
        }

        // 本次企微返回中出現的聯絡人 key，同步結束後用於清理過期記錄
        Set<String> currentKeys = new HashSet<>();
        List<SysSchoolFamilyContact> toInsert = new ArrayList<>();
        List<SysSchoolFamilyContact> toUpdate = new ArrayList<>();

        // 遍歷企微返回的家長列表，每個家長可關聯多名學生
        for (int i = 0; i < parentsArray.size(); i++) {
            // 解析家長信息
            JSONObject parentObj = parentsArray.getJSONObject(i);
            String parentUserId = parentObj.getString("parent_userid");
            String mobile = parentObj.getString("mobile");
            String externalUserid = parentObj.getString("external_userid");
            if (parentUserId == null) {
                continue;
            }

            // 獲取到學生信息
            JSONArray childrenArray = parentObj.getJSONArray("children");
            if (childrenArray == null || childrenArray.isEmpty()) {
                continue;
            }

            // 同一家長下的每名學生對應一條聯絡人記錄
            for (int j = 0; j < childrenArray.size(); j++) {
                // 解析學生信息
                JSONObject childObj = childrenArray.getJSONObject(j);
                String studentUserId = childObj.getString("student_userid");
                String relationDesc = childObj.getString("relation");
                String studentName = childObj.getString("name");
                if (studentUserId == null) {
                    continue;
                }

                // 生成聯絡人唯一鍵，用於同部門內比對與去重。
                String key = buildContactKey(parentUserId, studentUserId);
                currentKeys.add(key);

                // 將企微家校通訊錄返回的家長-學生資料組裝為聯絡人實體。
                SysSchoolFamilyContact wecomContact = buildContactEntity(
                        targetDepartmentId,
                        parentUserId,
                        studentUserId,
                        studentName,
                        relationDesc,
                        mobile,
                        externalUserid
                );

                SysSchoolFamilyContact dbContact = existingMap.get(key);
                if (dbContact == null) {
                    // 本地不存在 → 待新增
                    toInsert.add(wecomContact);
                } else if (needsUpdate(dbContact, wecomContact)) {
                    // 本地已存在但欄位有變 → 待更新
                    dbContact.setStudentName(wecomContact.getStudentName());
                    dbContact.setRelationDesc(wecomContact.getRelationDesc());
                    dbContact.setMobile(wecomContact.getMobile());
                    dbContact.setExternalUserid(wecomContact.getExternalUserid());
                    dbContact.setUpdateTime(LocalDateTime.now());
                    toUpdate.add(dbContact);
                }
            }
        }

        // 批量新增：每批最多 1000 條，避免單次 SQL 過大
        if (!toInsert.isEmpty()) {
            int batchSize = 1000;
            for (int i = 0; i < toInsert.size(); i += batchSize) {
                List<SysSchoolFamilyContact> subList = toInsert.subList(i, Math.min(i + batchSize, toInsert.size()));
                schoolFamilyContactMapper.batchInsert(subList);
            }
            logger.info("部門 ID {} 批量新增了 {} 條家校通訊錄聯絡人", targetDepartmentId, toInsert.size());
        }

        // 逐條更新有變動的聯絡人
        if (!toUpdate.isEmpty()) {
            for (SysSchoolFamilyContact contact : toUpdate) {
                schoolFamilyContactMapper.updateContact(contact);
            }
            logger.info("部門 ID {} 更新了 {} 條家校通訊錄聯絡人", targetDepartmentId, toUpdate.size());
        }

        // 清理該部門下企微已不存在的過期聯絡人（複用上方已查詢的 existingContacts，避免重複查庫）
        deleteObsoleteContactsForDepartment(targetDepartmentId, existingContacts, currentKeys);
    }

    /**
     * 刪除指定部門中，本次企微同步未返回的過期聯絡人。
     *
     * @param departmentId      班級部門 ID
     * @param existingContacts  同步開始前該部門的本地聯絡人列表（由調用方傳入）
     * @param currentKeys       本次企微返回的聯絡人 key 集合（parentUserId_studentUserId）
     */
    private void deleteObsoleteContactsForDepartment(Long departmentId,
                                                     List<SysSchoolFamilyContact> existingContacts,
                                                     Set<String> currentKeys) {
        if (existingContacts == null || existingContacts.isEmpty()) {
            return;
        }

        // 找出本地有、但本次企微未返回的記錄
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

    /**
     * 將企微家校通訊錄返回的家長-學生資料組裝為聯絡人實體。
     *
     * @param departmentId    所屬班級部門 ID
     * @param parentUserId    家長企微 userid
     * @param studentUserId   學生企微 userid
     * @param studentName     學生姓名
     * @param relation        家長與學生關係描述（如：父親、母親）
     * @param mobile          家長手機號
     * @param externalUserid  家長外部聯絡人 ID
     * @return 待寫入 {@code sys_school_family_contact} 的聯絡人實體
     */
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

    /**
     * 生成聯絡人唯一鍵，用於同部門內比對與去重。
     *
     * @param parentUserId  家長企微 userid
     * @param studentUserId 學生企微 userid
     * @return 格式為 {@code parentUserId_studentUserId}
     */
    private String buildContactKey(String parentUserId, String studentUserId) {
        return parentUserId + "_" + studentUserId;
    }

    /**
     * 判斷本地聯絡人是否需要根據企微最新資料更新。
     * <p>僅比對可變欄位：學生姓名、關係描述、手機號、外部 ID。</p>
     *
     * @param dbContact    本地已有記錄
     * @param wecomContact 企微返回的最新資料
     * @return 任一欄位不同時返回 {@code true}
     */
    private boolean needsUpdate(SysSchoolFamilyContact dbContact, SysSchoolFamilyContact wecomContact) {
        return isDifferent(dbContact.getStudentName(), wecomContact.getStudentName())
                || isDifferent(dbContact.getRelationDesc(), wecomContact.getRelationDesc())
                || isDifferent(dbContact.getMobile(), wecomContact.getMobile())
                || isDifferent(dbContact.getExternalUserid(), wecomContact.getExternalUserid());
    }

    /**
     * 比較兩個字串是否不同（{@code null} 視為相等）。
     *
     * @param s1 字串一
     * @param s2 字串二
     * @return 兩者皆為 {@code null} 時返回 {@code false}，否則按值比較
     */
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
