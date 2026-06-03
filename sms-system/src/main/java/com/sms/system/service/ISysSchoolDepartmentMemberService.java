package com.sms.system.service;

import com.sms.system.entity.SysSchoolDepartmentMember;

import java.util.List;

/**
 * 系統學校部門成員 Service 接口
 *
 */
public interface ISysSchoolDepartmentMemberService {

    /**
     * 批量查詢多個部門的成員列表
     *
     * @param departmentIds 部門 ID 列表
     * @return 成員列表
     */
    List<SysSchoolDepartmentMember> getMembersByDepartmentIds(List<Long> departmentIds);

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
     * @param members 成員列表
     * @return 影響行數
     */
    int batchAddMembers(List<SysSchoolDepartmentMember> members);

}
