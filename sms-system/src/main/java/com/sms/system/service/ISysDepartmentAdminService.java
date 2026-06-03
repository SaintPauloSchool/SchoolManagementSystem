package com.sms.system.service;

import com.sms.system.entity.SysDepartmentAdmin;
import java.util.List;

/**
 * 部門管理員Service接口
 */
public interface ISysDepartmentAdminService {

    /**
     * 批量保存部門管理員信息（處理新增和更新）
     *
     * @param admins 部門管理員列表
     */
    void batchSaveDepartmentAdmins(List<SysDepartmentAdmin> admins);
}
