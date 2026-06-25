package com.sms.system.service;

import com.sms.system.entity.SysDepartment;
import com.sms.system.entity.vo.SysDepartmentVO;

import java.util.List;

/**
 * 部門 Service 接口
 *
 */
public interface ISysDepartmentService {

    /**
     * 根據管理員權限獲取班級樹形結構（僅返回該用戶有權管理的部門）
     * 通過 sys_department_admin 查詢用戶管理的部門 ID，再過濾完整樹
     *
     * @param openUserId 企業微信 userid（當前登錄用戶）
     * @return 過濾後的學校層級樹形結構
     */
    List<SysDepartmentVO> getClassTreeByAdmin(String openUserId);

    /**
     * 根據管理員權限獲取班級樹形結構（帶家長學生關係）
     * 在 getClassTreeByAdmin 的基礎上爲 type=1 的班級加載家長學生關係數據
     *
     * @param openUserId 企業微信 userid（當前登錄用戶）
     * @return 過濾後的帶家長學生關係的樹形結構
     */
    List<SysDepartmentVO> getClassTreeWithParentsByAdmin(String openUserId);

    /**
     * 批量保存部門信息
     *
     * @param departments 部門列表
     */
    void batchSaveDepartments(List<SysDepartment> departments);

    /**
     * 獲取班級部門 ID
     *
     * @return 班級部門 ID 列表
     */
    List<Long> getClassDepartmentId();

    /**
     * 同步企業微信家校通訊錄部門與管理員數據
     * @param departmentJson 微信接口返回的部門 JSON 數據
     */
    void syncSchoolDepartmentData(com.alibaba.fastjson.JSONObject departmentJson);
}

