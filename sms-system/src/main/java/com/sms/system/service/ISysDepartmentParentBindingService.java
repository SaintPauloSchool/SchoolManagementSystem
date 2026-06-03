package com.sms.system.service;

import com.alibaba.fastjson.JSONArray;
import com.sms.system.entity.SysDepartmentParentBinding;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 部門家長綁定服務層接口
 */
public interface ISysDepartmentParentBindingService {

    /**
     * 獲取所有綁定的家長用戶ID
     * @return 家長ID列表
     */
    List<String> getAllParentUserIds();

    /**
     * 根據部門ID查詢綁定關係
     * @param departmentId 部門ID
     * @return 綁定列表
     */
    List<SysDepartmentParentBinding> selectByDepartmentId(Long departmentId);

    /**
     * 處理家長孩子關係（同步用）
     * @param departmentId 部門ID
     * @param parentUserId 家長用戶ID
     * @param childrenArray 孩子信息JSON數組
     * @param existingBindingMap 現有的綁定Map
     */
    void processParentChildren(Long departmentId, String parentUserId, JSONArray childrenArray,
                               Map<String, SysDepartmentParentBinding> existingBindingMap);

    /**
     * 刪除廢棄的家長綁定記錄
     * @param existingBindings 已有的綁定列表
     * @param currentParentUserIds 當前獲取到的家長ID集合
     * @param departmentId 部門ID
     */
    void deleteObsoleteParentBindings(List<SysDepartmentParentBinding> existingBindings,
                                      Set<String> currentParentUserIds,
                                      Long departmentId);
}
