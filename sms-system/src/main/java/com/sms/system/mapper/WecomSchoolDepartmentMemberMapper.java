package com.sms.system.mapper;

import com.sms.system.entity.WecomSchoolDepartmentMember;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * wecom學校部門成員 Mapper 接口
 *
 */
public interface WecomSchoolDepartmentMemberMapper {

    /**
     * 批量查詢多個部門的成員
     *
     * @param departmentIds 部門 ID 列表
     * @return 成員集合
     */
    List<WecomSchoolDepartmentMember> selectMembersByDepartmentIds(@Param("departmentIds") List<Long> departmentIds);

    /**
     * 根據 ID 批量查詢部門成員
     *
     * @param ids 成員 ID 列表
     * @return 成員集合
     */
    List<WecomSchoolDepartmentMember> selectMembersByIds(@Param("ids") List<Long> ids);

    /**
     * 根據企業微信 userid 字串查詢完整成員對象
     *
     * @param userid 企業微信 userid（如 "ZhangSan001"）
     * @return 成員對象，若不存在則返回 null
     */
    WecomSchoolDepartmentMember selectByUserid(@Param("userid") String userid);

    /**
     * 批量新增部門成員
     */
    int batchInsertSchoolDepartmentMembers(@Param("list") List<WecomSchoolDepartmentMember> list);

    /**
     * 根據主鍵 ID 列表批量刪除成員
     *
     * @param ids 成員 ID 列表
     * @return 影響的行數
     */
    int deleteMembersByIds(@Param("ids") List<Long> ids);
}
