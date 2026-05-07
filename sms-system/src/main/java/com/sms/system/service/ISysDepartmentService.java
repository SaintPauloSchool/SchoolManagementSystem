package com.sms.system.service;

import com.sms.system.entity.SysDepartment;

import java.util.List;

/**
 * 部门 Service 接口
 *
 */
public interface ISysDepartmentService {

    /**
     * 根据管理员权限获取班级树形结构（仅返回该用户有权管理的部门）
     * 通过 sys_department_admin 查询用户管理的部门 ID，再过滤完整树
     *
     * @param openUserId 企业微信 userid（当前登录用户）
     * @return 过滤后的学校层级树形结构
     */
    List<SysDepartment> getClassTreeByAdmin(String openUserId);

    /**
     * 根据管理员权限获取班级树形结构（带家长学生关系）
     * 在 getClassTreeByAdmin 的基础上为 type=1 的班级加载家长学生关系数据
     *
     * @param openUserId 企业微信 userid（当前登录用户）
     * @return 过滤后的带家长学生关系的树形结构
     */
    List<SysDepartment> getClassTreeWithParentsByAdmin(String openUserId);
}

