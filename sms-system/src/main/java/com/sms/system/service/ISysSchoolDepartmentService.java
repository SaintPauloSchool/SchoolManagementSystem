package com.sms.system.service;

import com.sms.system.entity.dto.SysSchoolDepartmentSaveDTO;
import com.sms.system.entity.vo.SysSchoolDepartmentVO;

import java.util.List;

/**
 * 系統學校部門 Service 接口
 *
 */
public interface ISysSchoolDepartmentService {

    /**
     * 獲取當前用戶擁有的學校部門樹（僅部門，不含人員）
     *
     * @param type        部門類型（1 學校部門 2 家校通訊錄）
     * @param ownerUserid 當前用戶企微 userid
     * @return 部門樹形結構
     */
    List<SysSchoolDepartmentVO> getSysSchoolDepartmentTree(Integer type, String ownerUserid);

    /**
     * 獲取當前用戶擁有的學校部門樹（包含人員作爲葉子節點）
     *
     * @param type        部門類型（1 學校部門 2 家校通訊錄）
     * @param ownerUserid 當前用戶企微 userid
     * @return 部門樹形結構，人員作爲負ID的葉節點
     */
    List<SysSchoolDepartmentVO> getSysSchoolDepartmentTreeWithMembers(Integer type, String ownerUserid);

    /**
     * 根據 ID 刪除學校部門（僅擁有者可刪）
     * 同時刪除該部門下的所有子部門和成員
     *
     * @param id          部門 ID
     * @param ownerUserid 當前用戶企微 userid
     * @return 結果
     */
    int deleteSysSchoolDepartmentById(Long id, String ownerUserid);

    /**
     * 新增部門（寫入擁有者）
     *
     * @param sysSchoolDepartmentSaveDTO 部門資訊
     * @param ownerUserid                當前用戶企微 userid
     * @return 結果
     */
    int insertSysSchoolDepartment(SysSchoolDepartmentSaveDTO sysSchoolDepartmentSaveDTO, String ownerUserid);

    /**
     * 修改部門（僅擁有者可改）
     *
     * @param sysSchoolDepartmentSaveDTO 部門資訊
     * @param ownerUserid                當前用戶企微 userid
     * @return 結果
     */
    int updateSysSchoolDepartment(SysSchoolDepartmentSaveDTO sysSchoolDepartmentSaveDTO, String ownerUserid);

    /**
     * 校驗部門是否屬於當前用戶；不屬於則拋出業務異常。
     *
     * @param departmentId 自定義部門 ID
     * @param ownerUserid  當前用戶企微 userid
     */
    void assertOwnedBy(Long departmentId, String ownerUserid);
}
