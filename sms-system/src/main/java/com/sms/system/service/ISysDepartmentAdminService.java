package com.sms.system.service;

import com.sms.system.entity.SysDepartmentAdmin;
import java.util.List;

/**
 * 部门管理员Service接口
 */
public interface ISysDepartmentAdminService {

    /**
     * 批量保存部门管理员信息（处理新增和更新）
     *
     * @param admins 部门管理员列表
     */
    void batchSaveDepartmentAdmins(List<SysDepartmentAdmin> admins);
}
