package com.sms.system.service;

import com.sms.system.entity.SysAdmin;
import com.sms.system.entity.dto.SysAdminBatchSaveDTO;
import com.sms.system.entity.dto.SysAdminQueryDTO;
import com.sms.system.entity.dto.SysAdminUpdateDTO;
import com.sms.system.entity.vo.SysAdminBatchInsertResultVO;
import com.sms.system.entity.vo.SysAdminCurrentUserVO;
import com.sms.system.entity.vo.SysAdminVO;

import java.util.List;

/**
 * 系統管理員 Service 接口
 */
public interface ISysAdminService {

    /**
     * 驗證用戶是否「非」管理員（含超級管理員與普通管理員，且狀態正常）
     */
    boolean isNotAdmin(String userId);

    /**
     * 驗證用戶是否「非」超級管理員
     */
    boolean isNotSuperAdmin(String userId);

    /**
     * 根據 userId 查詢
     */
    SysAdmin selectByUserId(String userId);

    /**
     * 查詢當前用戶管理員權限
     */
    SysAdminCurrentUserVO selectCurrentUserInfo(String userId);

    /**
     * 條件列表
     */
    List<SysAdminVO> selectList(SysAdminQueryDTO queryDTO);

    /**
     * 詳情
     */
    SysAdminVO selectById(Long id);

    /**
     * 從 WeCom 選人批量新增
     */
    SysAdminBatchInsertResultVO batchInsert(SysAdminBatchSaveDTO saveDTO);

    /**
     * 修改
     *
     * @param operatorUserId 當前操作者 userId（用於保護規則）
     */
    int updateAdmin(SysAdminUpdateDTO updateDTO, String operatorUserId);

    /**
     * 批量刪除
     *
     * @param operatorUserId 當前操作者 userId
     */
    int deleteByIds(Long[] ids, String operatorUserId);
}
