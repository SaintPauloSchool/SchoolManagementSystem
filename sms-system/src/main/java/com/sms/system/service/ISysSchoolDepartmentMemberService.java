package com.sms.system.service;

import com.sms.system.entity.dto.SysSchoolDepartmentMemberBatchSaveDTO;
import com.sms.system.entity.dto.SysSchoolDepartmentMemberQueryDTO;
import com.sms.system.entity.vo.SysSchoolDepartmentMemberVO;

import java.util.List;

/**
 * 系統學校部門成員 Service 接口
 *
 */
public interface ISysSchoolDepartmentMemberService {

    /**
     * 批量查詢多個部門的成員列表
     *
     * @param sysSchoolDepartmentMemberQueryDTO 查詢條件
     * @return 成員列表
     */
    List<SysSchoolDepartmentMemberVO> getMembersByDepartmentIds(SysSchoolDepartmentMemberQueryDTO sysSchoolDepartmentMemberQueryDTO);

    /**
     * 根據 ID 刪除部門成員
     *
     * @param id 成員 ID
     * @return 影響行數
     */
    int deleteMemberById(Long id);

    /**
     * 批量添加部門成員
     *
     * @param sysSchoolDepartmentMemberBatchSaveDTO 批量新增請求
     * @return 影響行數
     */
    int batchAddMembers(SysSchoolDepartmentMemberBatchSaveDTO sysSchoolDepartmentMemberBatchSaveDTO);

}
