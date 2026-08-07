package com.sms.system.mapper;

import com.sms.system.entity.SysSchoolDepartmentMember;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系統學校部門成員 Mapper 接口
 *
 */
public interface SysSchoolDepartmentMemberMapper {

    /**
     * 批量查詢多個部門的成員
     *
     * @param departmentIds 部門 ID 列表
     * @return 成員集合
     */
    List<SysSchoolDepartmentMember> selectMembersByDepartmentIds(@Param("departmentIds") List<Long> departmentIds);

    /**
     * 根據 ID 批量查詢部門成員
     * @param ids
     * @return
     */
    List<SysSchoolDepartmentMember> selectMembersByIds(@Param("ids") List<Long> ids);

    /**
     * 根據 ID 刪除部門成員
     *
     * @param id 成員 ID
     * @return 影響行數
     */
    int deleteMemberById(@Param("id") Long id);

    /**
     * 根據部門 ID 刪除成員
     *
     * @param departmentId 部門 ID
     * @return 影響行數
     */
    int deleteByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * 批量插入部門成員
     *
     * @param members 成員列表
     * @return 影響行數
     */
    int batchInsertMembers(@Param("members") List<SysSchoolDepartmentMember> members);

    /**
     * 根據 userid 批量查詢家校通訊錄成員（type=2）
     */
    List<SysSchoolDepartmentMember> selectMembersByUserids(@Param("userids") List<String> userids);

    /**
     * 查詢屬於自定義家校通訊錄（type=2）的成員 userid
     */
    List<String> selectHomeSchoolUseridsByUserids(@Param("userids") List<String> userids);

}
