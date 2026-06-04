package com.sms.system.service;

/**
 * 家長學生關係Service接口
 */
public interface ISysParentStudentRelationService {

    /**
     * 建立並儲存家長與學生的關聯記錄
     *
     * @param parentUserId     家長 User ID
     * @param studentUserId    學生 User ID
     * @param studentName      學生姓名
     * @param relationDesc     關係描述（如：父親、母親）
     * @param mobile           家長手機號
     * @param externalUserId   外部聯絡人 ID
     */
    void createAndSaveParentStudentRelation(String parentUserId, String studentUserId, String studentName, String relationDesc, String mobile, String externalUserId);

    /**
     * 同步企業微信家校通訊錄家長與學生關聯數據
     * @param targetDepartmentId 目標班級部門ID
     * @param parentJson 微信接口返回的家長列表數據
     */
    void syncParentStudentRelationData(Long targetDepartmentId, com.alibaba.fastjson.JSONObject parentJson);

    /**
     * 全局清理已不在任何部門綁定中的孤立家長學生關係記錄
     */
    void deleteOrphanRelations();

}
