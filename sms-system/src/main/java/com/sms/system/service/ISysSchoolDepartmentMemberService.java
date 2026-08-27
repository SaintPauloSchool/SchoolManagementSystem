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
     * 批量查詢多個部門的成員列表（僅擁有者可查）
     */
    List<SysSchoolDepartmentMemberVO> getMembersByDepartmentIds(
            SysSchoolDepartmentMemberQueryDTO sysSchoolDepartmentMemberQueryDTO,
            String ownerUserid);

    /**
     * 根據 ID 刪除部門成員（僅擁有者可刪）
     */
    int deleteMemberById(Long id, String ownerUserid);

    /**
     * 批量添加部門成員（僅擁有者可加）
     */
    int batchAddMembers(SysSchoolDepartmentMemberBatchSaveDTO sysSchoolDepartmentMemberBatchSaveDTO,
                        String ownerUserid);
}
