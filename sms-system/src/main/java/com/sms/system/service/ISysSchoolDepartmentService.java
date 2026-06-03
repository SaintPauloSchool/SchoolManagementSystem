package com.sms.system.service;

import com.sms.system.entity.SysSchoolDepartment;

import java.util.List;

/**
 * 系統學校部門 Service 接口
 *
 */
public interface ISysSchoolDepartmentService {

    /**
     * 獲取學校部門樹形結構（僅部門，不含人員）
     *
     * @param type 部門類型（1 學校部門 2 家校通訊錄）
     * @return 部門樹形結構
     */
    List<SysSchoolDepartment> getSysSchoolDepartmentTree(Integer type);

    /**
     * 獲取學校部門樹形結構（包含人員作爲葉子節點）
     *
     * @param type 部門類型（1 學校部門 2 家校通訊錄）
     * @return 部門樹形結構，人員作爲負ID的葉節點
     */
    List<SysSchoolDepartment> getSysSchoolDepartmentTreeWithMembers(Integer type);

    /**
     * 根據 ID 刪除學校部門
     * 同時刪除該部門下的所有子部門和成員
     *
     * @param id 部門 ID
     * @return 結果
     */
    int deleteSysSchoolDepartmentById(Long id);

    /**
     * 新增部門
     *
     * @param department 部門信息
     * @return 結果
     */
    int insertSysSchoolDepartment(SysSchoolDepartment department);

    /**
     * 修改部門
     *
     * @param department 部門信息
     * @return 結果
     */
    int updateSysSchoolDepartment(SysSchoolDepartment department);

}
