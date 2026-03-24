package com.sp.system.service;

import com.sp.common.core.domain.entity.SysDepartment;

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
     * 查询所有班级（type=1）
     *
     * @return 班级集合
     */
    List<SysDepartment> selectAllClasses();
}
