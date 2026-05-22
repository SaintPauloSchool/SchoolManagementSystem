package com.sms.system.service;

import com.alibaba.fastjson.JSONArray;
import com.sms.system.entity.SysDepartmentParentBinding;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 部门家长绑定服务层接口
 */
public interface ISysDepartmentParentBindingService {

    /**
     * 获取所有绑定的家长用户ID
     * @return 家长ID列表
     */
    List<String> getAllParentUserIds();

    /**
     * 根据部门ID查询绑定关系
     * @param departmentId 部门ID
     * @return 绑定列表
     */
    List<SysDepartmentParentBinding> selectByDepartmentId(Long departmentId);

    /**
     * 处理家长孩子关系（同步用）
     * @param departmentId 部门ID
     * @param parentUserId 家长用户ID
     * @param childrenArray 孩子信息JSON数组
     * @param existingBindingMap 现有的绑定Map
     */
    void processParentChildren(Long departmentId, String parentUserId, JSONArray childrenArray,
                               Map<String, SysDepartmentParentBinding> existingBindingMap);

    /**
     * 删除废弃的家长绑定记录
     * @param existingBindings 已有的绑定列表
     * @param currentParentUserIds 当前获取到的家长ID集合
     * @param departmentId 部门ID
     */
    void deleteObsoleteParentBindings(List<SysDepartmentParentBinding> existingBindings,
                                      Set<String> currentParentUserIds,
                                      Long departmentId);
}
