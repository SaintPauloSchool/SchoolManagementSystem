package com.sms.system.mapper;

import com.sms.system.entity.SysSchoolFamilyContact;
import com.sms.system.entity.dto.SysWecomStudentDTO;
import com.sms.system.entity.vo.SysSchoolFamilyContactVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 家校通訊錄聯絡人 Mapper 接口
 * 對應表 {@code sys_school_family_contact}
 */
public interface SysSchoolFamilyContactMapper {

    /**
     * 按家長 userid 批量查詢聯絡人
     *
     * @param parentUserIds 家長企微 userid 列表
     * @return 聯絡人列表
     */
    List<SysSchoolFamilyContact> selectByParentUserIds(@Param("parentUserIds") List<String> parentUserIds);

    /**
     * 按主鍵 ID 批量查詢聯絡人
     *
     * @param ids 聯絡人主鍵 ID 列表
     * @return 聯絡人列表
     */
    List<SysSchoolFamilyContact> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 按家長 userid 與學生 userid 批量查詢聯絡人
     *
     * @param parentUserIds  家長企微 userid 列表
     * @param studentUserIds 學生企微 userid 列表
     * @return 聯絡人列表
     */
    List<SysSchoolFamilyContact> selectByParentAndStudentUserIds(
            @Param("parentUserIds") List<String> parentUserIds,
            @Param("studentUserIds") List<String> studentUserIds);

    /**
     * 查詢指定班級部門下的全部聯絡人
     *
     * @param departmentId 班級部門 ID（type=1）
     * @return 該部門下的聯絡人列表
     */
    List<SysSchoolFamilyContact> selectByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * 按部門 ID 列表批量查詢聯絡人
     * <p>用於部門樹展開家長節點、通知按班級/部門選人</p>
     *
     * @param departmentIds 部門 ID 列表
     * @return 聯絡人列表
     */
    List<SysSchoolFamilyContact> selectByDepartmentIds(@Param("departmentIds") List<Long> departmentIds);

    /**
     * 按學生 userid 批量查詢聯絡人
     * <p>一家長多學生時，用於解析同一 parent-student 對應的部門</p>
     *
     * @param studentUserIds 學生企微 userid 列表
     * @return 聯絡人列表
     */
    List<SysSchoolFamilyContact> selectByStudentUserIds(@Param("studentUserIds") List<String> studentUserIds);

    /**
     * 按班級部門 ID 列表查詢去重家長 userid。
     *
     * @param departmentIds 班級部門 ID 列表（type=1）
     * @return 家長企微 userid 列表
     */
    List<String> selectParentUserIdsByDepartmentIds(@Param("departmentIds") List<Long> departmentIds);

    /**
     * 批量新增聯絡人
     *
     * @param contacts 待插入的聯絡人列表
     * @return 影響行數
     */
    int batchInsert(@Param("contacts") List<SysSchoolFamilyContact> contacts);

    /**
     * 更新聯絡人可變欄位（姓名、關係、手機、外部 ID 等）
     *
     * @param contact 含主鍵及待更新欄位的聯絡人
     * @return 影響行數
     */
    int updateContact(SysSchoolFamilyContact contact);

    /**
     * 按 parent + student + department 組合批量刪除聯絡人
     *
     * @param contacts 待刪除的聯絡人列表（需含 parentUserId、studentUserId、departmentId）
     * @return 影響行數
     */
    int deleteBatch(@Param("contacts") List<SysSchoolFamilyContact> contacts);

    /**
     * 按主鍵 ID 批量刪除聯絡人
     *
     * @param ids 聯絡人主鍵 ID 列表
     * @return 影響行數
     */
    int deleteBatchByIds(@Param("ids") List<Long> ids);

    /**
     * 查詢全部聯絡人記錄
     *
     * @return 全表聯絡人列表
     */
    List<SysSchoolFamilyContact> selectAllContacts();

    /**
     * 查詢聯絡人列表並關聯班級代碼
     * <p>JOIN {@code sys_department}、{@code class_section}，供學生匹配等場景使用</p>
     *
     * @return 含班級代碼的聯絡人 VO 列表
     */
    List<SysSchoolFamilyContactVO> selectSchoolFamilyContactWithClassList();

    /**
     * 查詢企微學生候選列表（含篩選條件，供 PageHelper 分頁）
     *
     * @param query 姓名、手機、班級、學籍 studentId 等查詢條件
     * @return 候選聯絡人 VO 列表
     */
    List<SysSchoolFamilyContactVO> selectWecomCandidates(SysWecomStudentDTO query);

    /**
     * 按班級部門 ID 查詢已匹配學籍的家校聯絡人。
     * <p>關聯鏈：部門 name → class_section → student_info.class_section → sys_student_match → parent_user_id + student_user_id</p>
     *
     * @param departmentIds           班級部門 ID 列表（type=1）
     * @param studentProfilesDatabase 學籍庫名
     * @return 已匹配學籍的聯絡人 VO 列表（含 studentId 等關聯字段）
     */
    List<SysSchoolFamilyContactVO> selectMatchedContactsByDepartmentIds(
            @Param("departmentIds") List<Long> departmentIds,
            @Param("studentProfilesDatabase") String studentProfilesDatabase);
}
