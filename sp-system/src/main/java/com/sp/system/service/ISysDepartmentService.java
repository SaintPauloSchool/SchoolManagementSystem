package com.sp.system.service;

import com.sp.system.entity.SysDepartment;

import java.util.List;

/**
 * 部门 Service 接口
 *
 */
public interface ISysDepartmentService {

    /**
     * 根据类型查询部门列表
     *
     * @param type 部门类型
     * @return 部门集合
     */
    List<SysDepartment> selectByType(Integer type);

    /**
     * 根据父级 ID 查询子部门
     *
     * @param parentId 父级 ID
     * @return 部门集合
     */
    List<SysDepartment> selectByParentId(Integer parentId);

    /**
     * 获取班级树形结构（使用 Stream 流构建）
     * 层级顺序：type 5(学校) → type 4(校区) → type 3(学段) → type 2(年级) → type 1(班级)
     *
     * @return 学校层级的树形结构
     */
    List<SysDepartment> getClassTree();

    /**
     * 获取班级树形结构（带家长学生关系，用于学生/家长选择器）
     * 在 getClassTree 的基础上为 type=1 的班级加载家长学生关系数据
     *
     * @return 带家长学生关系的树形结构
     */
    List<SysDepartment> getClassTreeWithParents();

    /**
     * 查询所有班级（type=1）
     *
     * @return 班级集合
     */
    List<SysDepartment> selectAllClasses();
}
