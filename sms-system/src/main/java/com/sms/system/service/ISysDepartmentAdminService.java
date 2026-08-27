package com.sms.system.service;

import com.sms.system.entity.SysDepartmentAdmin;

import java.util.Collection;
import java.util.List;

/**
 * 部門管理員Service接口
 */
public interface ISysDepartmentAdminService {

    /**
     * 批量保存部門管理員資訊（處理新增和更新）
     *
     * @param admins 部門管理員列表
     */
    void batchSaveDepartmentAdmins(List<SysDepartmentAdmin> admins);

    /**
     * 以企微本次回傳為準差量同步部門管理員。
     * <p>僅新增缺失、更新變更、刪除過期記錄；未變化的行保留原主鍵 id，避免每日全量刪插推高自增。</p>
     *
     * @param admins              企微回傳的管理員列表（可為空）
     * @param syncedDepartmentIds 本次同步的全部部門 ID
     */
    void syncDepartmentAdminsFromWecom(List<SysDepartmentAdmin> admins, Collection<Long> syncedDepartmentIds);
}
