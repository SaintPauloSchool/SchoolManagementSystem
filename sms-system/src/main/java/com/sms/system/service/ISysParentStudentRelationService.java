package com.sms.system.service;

import com.sms.system.entity.SysParentStudentRelation;
import java.util.List;

/**
 * 家長學生關係Service接口
 */
public interface ISysParentStudentRelationService {


    /**
     * 同步企業微信家校通訊錄家長與學生關聯數據
     * @param targetDepartmentId 目標班級部門ID
     * @param parentJson 微信接口返回的家長列表數據
     * @return 該部門拉取到的所有家長學生關係實體列表
     */
    List<SysParentStudentRelation> syncParentStudentRelationData(Long targetDepartmentId, com.alibaba.fastjson.JSONObject parentJson);

    /**
     * 全局批量同步與比對家長學生關係
     *
     * @param wecomRelations 企業微信端獲取的所有關係列表
     * @param shouldDeleteObsolete 是否清理本地多餘/過期關係（當有任何班級同步失敗時為 false）
     */
    void syncAllParentStudentRelations(List<SysParentStudentRelation> wecomRelations, boolean shouldDeleteObsolete);

    /**
     * 全局清理已不在任何部門綁定中的孤立家長學生關係記錄
     */
    void deleteOrphanRelations();

}
