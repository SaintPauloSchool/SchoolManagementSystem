package com.sms.system.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.SysDepartment;
import com.sms.system.entity.SysDepartmentAdmin;
import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.entity.SysParentStudentRelation;
import com.sms.system.mapper.SysDepartmentAdminMapper;
import com.sms.system.mapper.SysDepartmentMapper;
import com.sms.system.mapper.SysDepartmentParentBindingMapper;
import com.sms.system.mapper.SysParentStudentRelationMapper;
import com.sms.system.service.ISysDepartmentAdminService;
import com.sms.system.service.ISysDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门 Service 业务层处理
 * 
 * @author sms
 * @date 2023-08-16
 */
@Service
public class SysDepartmentServiceImpl implements ISysDepartmentService {

    /** 部门类型常量 */
    private static final int TYPE_CLASS = 1;      // 班级
    private static final int TYPE_GRADE = 2;      // 年级
    private static final int TYPE_SCHOOL_SEGMENT = 3;  // 学段
    private static final int TYPE_CAMPUS = 4;     // 校区
    private static final int TYPE_SCHOOL = 5;     // 学校

    @Autowired
    private SysDepartmentMapper departmentMapper;

    @Autowired
    private ISysDepartmentAdminService departmentAdminService;

    @Autowired
    private SysDepartmentParentBindingMapper parentBindingMapper;

    @Autowired
    private SysParentStudentRelationMapper parentStudentRelationMapper;

    @Autowired
    private SysDepartmentAdminMapper departmentAdminMapper;

    /**
     * 根据管理员权限获取班级树形结构
     * 逻辑：查询 sys_department_admin 获取该用户管理的部门 ID 集合，
     * 然后对完整树做剪枝，只保留有权限部门及其子孙节点。
     *
     * @param openUserId 企业微信 userid
     * @return 过滤后的树形结构
     */
    @Override
    public List<SysDepartment> getClassTreeByAdmin(String openUserId) {
        // 1. 获取用户管理的部门 ID 集合
        Set<Long> adminDeptIds = getAdminDepartmentIds(openUserId);

        // 如果没有任何权限记录，返回空
        if (adminDeptIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 获取完整树
        List<SysDepartment> fullTree = getClassTree();

        // 3. 剪枝：只保留有权限的子树
        return filterTree(fullTree, adminDeptIds);
    }

    /**
     * 根据管理员权限获取班级树形结构（带家长学生关系）
     *
     * @param openUserId 企业微信 userid
     * @return 过滤后的带家长学生关系的树形结构
     */
    @Override
    public List<SysDepartment> getClassTreeWithParentsByAdmin(String openUserId) {
        // 1. 获取用户管理的部门 ID 集合
        Set<Long> adminDeptIds = getAdminDepartmentIds(openUserId);

        if (adminDeptIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 获取完整树（含家长学生关系）
        List<SysDepartment> fullTree = getClassTreeWithParents();

        // 3. 剪枝
        return filterTree(fullTree, adminDeptIds);
    }

    /**
     * 获取班级树形结构（私有辅助方法，供 ByAdmin 方法内部使用）
     * 层级顺序：type 5(学校) → type 4(校区) → type 3(学段) → type 2(年级) → type 1(班级)
     */
    private List<SysDepartment> getClassTree() {
        // 1. 查询所有部门数据
        List<SysDepartment> allDepartments = departmentMapper.selectAll();

        if (allDepartments == null || allDepartments.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 按 type 分组，便于快速查找
        Map<Integer, List<SysDepartment>> departmentsByType = allDepartments.stream()
                .filter(Objects::nonNull)
                .filter(dept -> dept.getType() != null)
                .collect(Collectors.groupingBy(SysDepartment::getType));

        // 3. 从最高层级开始构建（优先学校，其次校区）
        List<SysDepartment> rootNodes = departmentsByType.getOrDefault(TYPE_SCHOOL, Collections.emptyList());
        
        if (rootNodes.isEmpty()) {
            // 如果没有学校，尝试从校区开始
            rootNodes = departmentsByType.getOrDefault(TYPE_CAMPUS, Collections.emptyList());
            if (!rootNodes.isEmpty()) {
                buildAndSortTree(rootNodes, departmentsByType, TYPE_CAMPUS);
                return rootNodes;
            }
            return Collections.emptyList();
        }

        // 4. 构建树形结构并排序
        buildAndSortTree(rootNodes, departmentsByType, TYPE_SCHOOL);

        return rootNodes;
    }

    /**
     * 获取班级树形结构（带家长学生关系，私有辅助方法，供 ByAdmin 方法内部使用）
     */
    private List<SysDepartment> getClassTreeWithParents() {
        // 1. 获取基础树形结构
        List<SysDepartment> tree = getClassTree();
        
        // 2. 为 type=1 的班级添加家长学生关系数据
        if (!tree.isEmpty()) {
            loadParentStudentRelations(tree);
        }
        
        return tree;
    }

    /**
     * 查询该用户在 sys_department_admin 中管理的所有部门 ID
     *
     * @param openUserId 企业微信 userid
     * @return 部门 ID 集合（空集合表示无权限记录）
     */
    private Set<Long> getAdminDepartmentIds(String openUserId) {
        if (openUserId == null || openUserId.isEmpty()) {
            return Collections.emptySet();
        }
        List<SysDepartmentAdmin> adminRecords = departmentAdminMapper.selectByUserid(openUserId);
        if (adminRecords == null || adminRecords.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> ids = new HashSet<>();
        for (SysDepartmentAdmin record : adminRecords) {
            if (record.getDepartmentId() != null) {
                ids.add(record.getDepartmentId());
            }
        }
        return ids;
    }

    /**
     * 递归剪枝：保留与权限部门相关的节点
     * 规则：
     *  - 如果当前节点本身在权限集合中 → 保留该节点及其所有子节点
     *  - 否则，递归过滤子节点，如果子节点中有保留的节点则当前节点也保留
     *
     * @param nodes      待过滤的节点列表
     * @param adminIds   有权限的部门 ID 集合
     * @return 过滤后的节点列表
     */
    private List<SysDepartment> filterTree(List<SysDepartment> nodes, Set<Long> adminIds) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysDepartment> result = new ArrayList<>();
        for (SysDepartment node : nodes) {
            if (node == null) continue;

            if (adminIds.contains(node.getId())) {
                // 当前节点有权限，直接保留（子节点全部保留，无需继续过滤）
                result.add(node);
            } else {
                // 递归过滤子节点
                List<SysDepartment> filteredChildren = filterTree(node.getChildren(), adminIds);
                if (!filteredChildren.isEmpty()) {
                    // 有子节点保留，则当前节点也保留（作为路径节点），并替换子节点列表
                    SysDepartment copy = shallowCopy(node);
                    copy.setChildren(filteredChildren);
                    result.add(copy);
                }
                // 否则该节点及其子树均无权限，丢弃
            }
        }
        return result;
    }

    /**
     * 浅拷贝部门节点（不包含子节点，用于构建剪枝后的路径节点）
     */
    private SysDepartment shallowCopy(SysDepartment src) {
        SysDepartment copy = new SysDepartment();
        copy.setId(src.getId());
        copy.setParentId(src.getParentId());
        copy.setName(src.getName());
        copy.setType(src.getType());
        copy.setRegisterYear(src.getRegisterYear());
        copy.setStandardGrade(src.getStandardGrade());
        copy.setOrderNum(src.getOrderNum());
        copy.setIsGraduated(src.getIsGraduated());
        copy.setOpenGroupChat(src.getOpenGroupChat());
        copy.setGroupChatId(src.getGroupChatId());
        copy.setIsLeaf(src.getIsLeaf());
        return copy;
    }

    /**
     * 构建树形结构并排序（合并了构建和排序逻辑）
     * 从指定类型开始，逐级向下构建，并对每层进行排序
     *
     * @param currentLevel 当前层级的部门列表
     * @param departmentsByType 按类型分组的部门 Map
     * @param currentType 当前层级类型
     */
    private void buildAndSortTree(List<SysDepartment> currentLevel, 
                                   Map<Integer, List<SysDepartment>> departmentsByType,
                                   Integer currentType) {
        if (currentLevel == null || currentLevel.isEmpty()) {
            return;
        }

        // 对当前层级排序（按名称字母顺序）
        currentLevel.sort(Comparator.comparing(
                dept -> dept.getName() != null ? dept.getName() : "",
                String.CASE_INSENSITIVE_ORDER
        ));

        // 下一个层级（type 递减：5→4→3→2→1）
        Integer nextType = currentType - 1;
        
        // 如果没有下一个层级或者下一个层级没有数据，返回
        if (nextType < TYPE_CLASS || !departmentsByType.containsKey(nextType)) {
            return;
        }

        List<SysDepartment> nextLevelDepartments = departmentsByType.get(nextType);
        if (nextLevelDepartments == null) {
            return;
        }
        
        // 为当前层级的每个部门设置子部门
        for (SysDepartment currentDept : currentLevel) {
            if (currentDept == null || currentDept.getId() == null) {
                continue;
            }
            
            long currentId = currentDept.getId();
            List<SysDepartment> children = nextLevelDepartments.stream()
                    .filter(Objects::nonNull)
                    .filter(dept -> dept.getParentId() != null) 
                    .filter(dept -> dept.getParentId().longValue() == currentId)
                    .collect(Collectors.toList());

            if (!children.isEmpty()) {
                currentDept.setChildren(children);
                
                // 递归处理下一层级
                if (nextType > TYPE_CLASS) {
                    buildAndSortTree(children, departmentsByType, nextType);
                }
            }
        }
    }

    /**
     * 为班级节点加载家长学生关系数据
     * 批量查询所有班级的家长数据，避免 N+1 问题
     *
     * @param nodes 部门节点列表
     */
    private void loadParentStudentRelations(List<SysDepartment> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        
        // 1. 收集所有班级节点
        List<SysDepartment> classNodes = new ArrayList<>();
        collectClassNodes(nodes, classNodes);
        
        if (classNodes.isEmpty()) {
            return;
        }
        
        // 2. 收集所有班级 ID
        List<Long> classIds = classNodes.stream()
                .map(SysDepartment::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        if (classIds.isEmpty()) {
            return;
        }
        
        // 3. 批量查询所有班级的家长绑定关系
        List<SysDepartmentParentBinding> allBindings = parentBindingMapper.selectByDepartmentIds(classIds);
        
        if (allBindings == null || allBindings.isEmpty()) {
            return;
        }
        
        // 4. 按班级 ID 分组
        Map<Long, List<SysDepartmentParentBinding>> bindingsByClassId = allBindings.stream()
                .filter(Objects::nonNull)
                .filter(binding -> binding.getDepartmentId() != null)
                .collect(Collectors.groupingBy(SysDepartmentParentBinding::getDepartmentId));
        
        // 5. 获取所有不重复的家长用户 ID
        List<String> allParentUserIds = allBindings.stream()
                .filter(Objects::nonNull)
                .map(SysDepartmentParentBinding::getParentUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        if (allParentUserIds.isEmpty()) {
            return;
        }
        
        // 6. 批量查询所有家长学生关系
        List<SysParentStudentRelation> allRelations = parentStudentRelationMapper.selectByParentUserIds(allParentUserIds);
        
        if (allRelations == null || allRelations.isEmpty()) {
            return;
        }
        
        // 7. 按家长用户 ID 分组，便于快速查找
        Map<String, List<SysParentStudentRelation>> relationsByParentId = allRelations.stream()
                .filter(Objects::nonNull)
                .filter(relation -> relation.getParentUserId() != null)
                .collect(Collectors.groupingBy(SysParentStudentRelation::getParentUserId));
        
        // 8. 为每个班级节点设置家长数据
        for (SysDepartment classNode : classNodes) {
            List<SysDepartmentParentBinding> bindings = bindingsByClassId.get(classNode.getId());
            if (bindings == null || bindings.isEmpty()) {
                continue;
            }
            
            // 初始化 children 列表
            if (classNode.getChildren() == null) {
                classNode.setChildren(new ArrayList<>());
            }
            
            // 为该班级的每个家长添加节点
            for (SysDepartmentParentBinding binding : bindings) {
                String parentUserId = binding.getParentUserId();
                if (parentUserId == null) {
                    continue;
                }
                
                List<SysParentStudentRelation> relations = relationsByParentId.get(parentUserId);
                if (relations == null || relations.isEmpty()) {
                    continue;
                }
                
                // 只添加屬於該班級的學生關係記錄
                // 當一個家長綁定了多個班級的學生時，需要根據 binding.studentUserId 過濾，
                // 避免在某班顯示該家長與其他班學生的關係
                String bindingStudentUserId = binding.getStudentUserId();
                for (SysParentStudentRelation relation : relations) {
                    if (bindingStudentUserId != null && !bindingStudentUserId.equals(relation.getStudentUserId())) {
                        continue;
                    }
                    SysDepartment node = convertToDepartmentNode(relation);
                    classNode.getChildren().add(node);
                }
            }
        }
    }
    
    /**
     * 递归收集所有班级节点（type=1）
     *
     * @param nodes 部门节点列表
     * @param classNodes 用于存储收集到的班级节点
     */
    private void collectClassNodes(List<SysDepartment> nodes, List<SysDepartment> classNodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        
        for (SysDepartment dept : nodes) {
            if (dept == null) {
                continue;
            }
            
            // 如果是班级节点（type=1），添加到集合中
            if (dept.getType() != null && dept.getType() == TYPE_CLASS) {
                classNodes.add(dept);
            }
            
            // 递归处理子节点
            if (dept.getChildren() != null && !dept.getChildren().isEmpty()) {
                collectClassNodes(dept.getChildren(), classNodes);
            }
        }
    }

    /**
     * 将家长学生关系转换为部门节点
     * 节点名称格式：学生姓名 - 关系描述（如：张三 - 父亲）
     *
     * @param relation 家长学生关系
     * @return 部门节点
     */
    private SysDepartment convertToDepartmentNode(SysParentStudentRelation relation) {
        // 创建部门节点
        SysDepartment node = new SysDepartment();
        node.setId(relation.getId());
        node.setStudentUserId(relation.getStudentUserId());
        node.setParentUserId(relation.getParentUserId());
        
        // 构建显示名称：学生姓名 - 关系描述
        String studentName = relation.getStudentName() != null ? relation.getStudentName() : "未知";
        String relationDesc = relation.getRelationDesc() != null ? relation.getRelationDesc() : "";
        node.setName(studentName + (relationDesc.isEmpty() ? "" : "-" + relationDesc));
        
        node.setRelationDesc(relationDesc);
        node.setMobile(relation.getMobile());
        node.setIsLeaf(true);
        return node;
    }

    @Override
    public void batchSaveDepartments(List<SysDepartment> departments) {
        if (departments != null && !departments.isEmpty()) {
            departmentMapper.batchInsertDepartments(departments);
        }
    }

    @Override
    public List<Long> getClassDepartmentId() {
        return departmentMapper.selectClassDepartmentId();
    }

    /**
     * 同步學校部門數據
     * @param departmentJson 微信接口返回的部門 JSON 數據
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncSchoolDepartmentData(JSONObject departmentJson) {

        // 1. 获取部门数据
        if (departmentJson != null && departmentJson.getInteger("errcode") != null && departmentJson.getInteger("errcode") == 0) {
            JSONArray departmentsArray = departmentJson.getJSONArray("departments");
            // 2. 批量保存部门数据
            if (departmentsArray != null && !departmentsArray.isEmpty()) {
                List<SysDepartment> departmentsToSave = new ArrayList<>();
                // 遍历部门数据
                for (int i = 0; i < departmentsArray.size(); i++) {
                    JSONObject deptObj = departmentsArray.getJSONObject(i);

                    SysDepartment department = new SysDepartment();
                    department.setId(deptObj.getLong("id"));
                    department.setParentId(deptObj.getInteger("parentid"));
                    department.setName(deptObj.getString("name"));
                    department.setType(deptObj.getInteger("type"));
                    department.setRegisterYear(deptObj.getInteger("register_year"));
                    department.setStandardGrade(deptObj.getInteger("standard_grade"));
                    department.setOrderNum(deptObj.getInteger("order"));
                    department.setIsGraduated(deptObj.getInteger("is_graduated") != null && deptObj.getInteger("is_graduated") == 1);
                    department.setOpenGroupChat(deptObj.getInteger("open_group_chat") != null && deptObj.getInteger("open_group_chat") == 1);
                    department.setGroupChatId(deptObj.getString("group_chat_id"));

                    departmentsToSave.add(department);
                }
                // 批量保存部门数据
                batchSaveDepartments(departmentsToSave);
                // 3. 批量保存部门管理员数据
                List<SysDepartmentAdmin> allAdmins = new ArrayList<>();
                for (int i = 0; i < departmentsArray.size(); i++) {
                    JSONObject deptObj = departmentsArray.getJSONObject(i);
                    JSONArray adminsArray = deptObj.getJSONArray("department_admins");
                    
                    if (adminsArray != null && !adminsArray.isEmpty()) {
                        Long departmentId = deptObj.getLong("id");
                        
                        for (int j = 0; j < adminsArray.size(); j++) {
                            JSONObject adminObj = adminsArray.getJSONObject(j);
                            
                            SysDepartmentAdmin admin = new SysDepartmentAdmin();
                            admin.setDepartmentId(departmentId);
                            admin.setUserid(adminObj.getString("userid"));
                            admin.setType(adminObj.getInteger("type"));
                            admin.setSubject(adminObj.getString("subject"));
                            
                            allAdmins.add(admin);
                        }
                    }
                }

                // 批量保存部门管理员信息
                if (!allAdmins.isEmpty()) {
                    departmentAdminService.batchSaveDepartmentAdmins(allAdmins);
                }
            }
        }
    }
}
