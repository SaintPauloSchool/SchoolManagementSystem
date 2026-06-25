package com.sms.system.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.common.utils.bean.BeanCopyUtils;
import com.sms.system.entity.SysDepartment;
import com.sms.system.entity.SysDepartmentAdmin;
import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.entity.SysParentStudentRelation;
import com.sms.system.mapper.SysDepartmentAdminMapper;
import com.sms.system.mapper.SysDepartmentMapper;
import com.sms.system.mapper.SysDepartmentParentBindingMapper;
import com.sms.system.mapper.SysParentStudentRelationMapper;
import com.sms.system.entity.vo.SysDepartmentVO;
import com.sms.system.service.ISysDepartmentAdminService;
import com.sms.system.service.ISysDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 部門 Service 業務層處理
 * 
 * @author sms
 * @date 2023-08-16
 */
@Service
public class SysDepartmentServiceImpl implements ISysDepartmentService {

    /** 部門/聯絡人節點按名稱排序（繁體中文） */
    private static final Collator NAME_COLLATOR = Collator.getInstance(Locale.TRADITIONAL_CHINESE);

    static {
        NAME_COLLATOR.setStrength(Collator.PRIMARY);
    }

    private static final int TYPE_CLASS = 1;      // 班級
    private static final int TYPE_GRADE = 2;      // 年級
    private static final int TYPE_SCHOOL_SEGMENT = 3;  // 學段
    private static final int TYPE_CAMPUS = 4;     // 校區
    private static final int TYPE_SCHOOL = 5;     // 學校

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
     * 根據管理員權限獲取班級樹形結構
     * 邏輯：查詢 sys_department_admin 獲取該用戶管理的部門 ID 集合，
     * 然後對完整樹做剪枝，只保留有權限部門及其子孫節點。
     *
     * @param openUserId 企業微信 userid
     * @return 過濾後的樹形結構
     */
    @Override
    public List<SysDepartmentVO> getClassTreeByAdmin(String openUserId) {
        // 1. 獲取用戶管理的部門 ID 集合
        Set<Long> adminDeptIds = getAdminDepartmentIds(openUserId);

        // 如果沒有任何權限記錄，返回空
        if (adminDeptIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 獲取完整樹
        List<SysDepartment> fullTree = getClassTree();

        // 3. 剪枝：只保留有權限的子樹
        return BeanCopyUtils.copyTree(filterTree(fullTree, adminDeptIds), SysDepartmentVO.class,
                SysDepartment::getChildren, SysDepartmentVO::setChildren);
    }

    /**
     * 根據管理員權限獲取班級樹形結構（帶家長學生關係）
     *
     * @param openUserId 企業微信 userid
     * @return 過濾後的帶家長學生關係的樹形結構
     */
    @Override
    public List<SysDepartmentVO> getClassTreeWithParentsByAdmin(String openUserId) {
        // 1. 獲取用戶管理的部門 ID 集合
        Set<Long> adminDeptIds = getAdminDepartmentIds(openUserId);

        if (adminDeptIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 獲取完整樹（含家長學生關係）
        List<SysDepartment> fullTree = getClassTreeWithParents();

        // 3. 剪枝
        return BeanCopyUtils.copyTree(filterTree(fullTree, adminDeptIds), SysDepartmentVO.class,
                SysDepartment::getChildren, SysDepartmentVO::setChildren);
    }

    /**
     * 獲取班級樹形結構（私有輔助方法，供 ByAdmin 方法內部使用）
     * 層級順序：type 5(學校) → type 4(校區) → type 3(學段) → type 2(年級) → type 1(班級)
     */
    private List<SysDepartment> getClassTree() {
        // 1. 查詢所有部門數據
        List<SysDepartment> allDepartments = departmentMapper.selectAll();

        if (allDepartments == null || allDepartments.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 按 type 分組，便於快速查找
        Map<Integer, List<SysDepartment>> departmentsByType = allDepartments.stream()
                .filter(Objects::nonNull)
                .filter(dept -> dept.getType() != null)
                .collect(Collectors.groupingBy(SysDepartment::getType));

        // 3. 從最高層級開始構建（優先學校，其次校區）
        List<SysDepartment> rootNodes = departmentsByType.getOrDefault(TYPE_SCHOOL, Collections.emptyList());
        
        if (rootNodes.isEmpty()) {
            // 如果沒有學校，嘗試從校區開始
            rootNodes = departmentsByType.getOrDefault(TYPE_CAMPUS, Collections.emptyList());
            if (!rootNodes.isEmpty()) {
                buildAndSortTree(rootNodes, departmentsByType, TYPE_CAMPUS);
                return rootNodes;
            }
            return Collections.emptyList();
        }

        // 4. 構建樹形結構並排序
        buildAndSortTree(rootNodes, departmentsByType, TYPE_SCHOOL);

        return rootNodes;
    }

    /**
     * 獲取班級樹形結構（帶家長學生關係，私有輔助方法，供 ByAdmin 方法內部使用）
     */
    private List<SysDepartment> getClassTreeWithParents() {
        // 1. 獲取基礎樹形結構
        List<SysDepartment> tree = getClassTree();
        
        // 2. 爲 type=1 的班級添加家長學生關係數據
        if (!tree.isEmpty()) {
            loadParentStudentRelations(tree);
        }
        
        return tree;
    }

    /**
     * 查詢該用戶在 sys_department_admin 中管理的所有部門 ID
     *
     * @param openUserId 企業微信 userid
     * @return 部門 ID 集合（空集合表示無權限記錄）
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
     * 遞歸剪枝：保留與權限部門相關的節點
     * 規則：
     *  - 如果當前節點本身在權限集合中 → 保留該節點及其所有子節點
     *  - 否則，遞歸過濾子節點，如果子節點中有保留的節點則當前節點也保留
     *
     * @param nodes      待過濾的節點列表
     * @param adminIds   有權限的部門 ID 集合
     * @return 過濾後的節點列表
     */
    private List<SysDepartment> filterTree(List<SysDepartment> nodes, Set<Long> adminIds) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysDepartment> result = new ArrayList<>();
        for (SysDepartment node : nodes) {
            if (node == null) continue;

            if (adminIds.contains(node.getId())) {
                // 當前節點有權限，直接保留（子節點全部保留，無需繼續過濾）
                result.add(node);
            } else {
                // 遞歸過濾子節點
                List<SysDepartment> filteredChildren = filterTree(node.getChildren(), adminIds);
                if (!filteredChildren.isEmpty()) {
                    // 有子節點保留，則當前節點也保留（作爲路徑節點），並替換子節點列表
                    SysDepartment copy = shallowCopy(node);
                    copy.setChildren(filteredChildren);
                    result.add(copy);
                }
                // 否則該節點及其子樹均無權限，丟棄
            }
        }
        return result;
    }

    /**
     * 淺拷貝部門節點（不包含子節點，用於構建剪枝後的路徑節點）
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
     * 構建樹形結構並排序（合併了構建和排序邏輯）
     * 從指定類型開始，逐級向下構建，並對每層進行排序
     *
     * @param currentLevel 當前層級的部門列表
     * @param departmentsByType 按類型分組的部門 Map
     * @param currentType 當前層級類型
     */
    private void buildAndSortTree(List<SysDepartment> currentLevel, 
                                   Map<Integer, List<SysDepartment>> departmentsByType,
                                   Integer currentType) {
        if (currentLevel == null || currentLevel.isEmpty()) {
            return;
        }

        // 對當前層級排序（按名稱）
        currentLevel.sort(Comparator.comparing(
                dept -> dept.getName() != null ? dept.getName() : "",
                NAME_COLLATOR
        ));

        // 下一個層級（type 遞減：5→4→3→2→1）
        Integer nextType = currentType - 1;
        
        // 如果沒有下一個層級或者下一個層級沒有數據，返回
        if (nextType < TYPE_CLASS || !departmentsByType.containsKey(nextType)) {
            return;
        }

        List<SysDepartment> nextLevelDepartments = departmentsByType.get(nextType);
        if (nextLevelDepartments == null) {
            return;
        }
        
        // 爲當前層級的每個部門設置子部門
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
                
                // 遞歸處理下一層級
                if (nextType > TYPE_CLASS) {
                    buildAndSortTree(children, departmentsByType, nextType);
                }
            }
        }
    }

    /**
     * 爲班級節點加載家長學生關係數據
     * 批量查詢所有班級的家長數據，避免 N+1 問題
     *
     * @param nodes 部門節點列表
     */
    private void loadParentStudentRelations(List<SysDepartment> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        
        // 1. 收集所有班級節點
        List<SysDepartment> classNodes = new ArrayList<>();
        collectClassNodes(nodes, classNodes);
        
        if (classNodes.isEmpty()) {
            return;
        }
        
        // 2. 收集所有班級 ID
        List<Long> classIds = classNodes.stream()
                .map(SysDepartment::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        if (classIds.isEmpty()) {
            return;
        }
        
        // 3. 批量查詢所有班級的家長綁定關係
        List<SysDepartmentParentBinding> allBindings = parentBindingMapper.selectByDepartmentIds(classIds);
        
        if (allBindings == null || allBindings.isEmpty()) {
            return;
        }
        
        // 4. 按班級 ID 分組
        Map<Long, List<SysDepartmentParentBinding>> bindingsByClassId = allBindings.stream()
                .filter(Objects::nonNull)
                .filter(binding -> binding.getDepartmentId() != null)
                .collect(Collectors.groupingBy(SysDepartmentParentBinding::getDepartmentId));
        
        // 5. 獲取所有不重複的家長用戶 ID
        List<String> allParentUserIds = allBindings.stream()
                .filter(Objects::nonNull)
                .map(SysDepartmentParentBinding::getParentUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        if (allParentUserIds.isEmpty()) {
            return;
        }
        
        // 6. 批量查詢所有家長學生關係
        List<SysParentStudentRelation> allRelations = parentStudentRelationMapper.selectByParentUserIds(allParentUserIds);
        
        if (allRelations == null || allRelations.isEmpty()) {
            return;
        }
        
        // 7. 按家長用戶 ID 分組，便於快速查找
        Map<String, List<SysParentStudentRelation>> relationsByParentId = allRelations.stream()
                .filter(Objects::nonNull)
                .filter(relation -> relation.getParentUserId() != null)
                .collect(Collectors.groupingBy(SysParentStudentRelation::getParentUserId));
        
        // 8. 爲每個班級節點設置家長數據
        for (SysDepartment classNode : classNodes) {
            List<SysDepartmentParentBinding> bindings = bindingsByClassId.get(classNode.getId());
            if (bindings == null || bindings.isEmpty()) {
                continue;
            }
            
            // 初始化 children 列表
            if (classNode.getChildren() == null) {
                classNode.setChildren(new ArrayList<>());
            }
            
            // 爲該班級的每個家長添加節點
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
                    SysDepartment node = convertToDepartmentNode(relation, classNode.getId());
                    classNode.getChildren().add(node);
                }
            }
            sortChildrenByName(classNode);
        }
    }

    /** 按名稱對節點的子列表排序 */
    private void sortChildrenByName(SysDepartment node) {
        if (node == null || node.getChildren() == null || node.getChildren().size() < 2) {
            return;
        }
        node.getChildren().sort(Comparator.comparing(
                dept -> dept.getName() != null ? dept.getName() : "",
                NAME_COLLATOR
        ));
    }
    
    /**
     * 遞歸收集所有班級節點（type=1）
     *
     * @param nodes 部門節點列表
     * @param classNodes 用於存儲收集到的班級節點
     */
    private void collectClassNodes(List<SysDepartment> nodes, List<SysDepartment> classNodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        
        for (SysDepartment dept : nodes) {
            if (dept == null) {
                continue;
            }
            
            // 如果是班級節點（type=1），添加到集合中
            if (dept.getType() != null && dept.getType() == TYPE_CLASS) {
                classNodes.add(dept);
            }
            
            // 遞歸處理子節點
            if (dept.getChildren() != null && !dept.getChildren().isEmpty()) {
                collectClassNodes(dept.getChildren(), classNodes);
            }
        }
    }

    /**
     * 將家長學生關係轉換爲部門節點
     * 節點名稱格式：學生姓名 - 關係描述（如：張三 - 父親）
     *
     * @param relation 家長學生關係
     * @param classDepartmentId 所屬家長班級部門 ID
     * @return 部門節點
     */
    private SysDepartment convertToDepartmentNode(SysParentStudentRelation relation, Long classDepartmentId) {
        // 創建部門節點
        SysDepartment node = new SysDepartment();
        node.setId(relation.getId());
        node.setStudentUserId(relation.getStudentUserId());
        node.setParentUserId(relation.getParentUserId());
        node.setClassDepartmentId(classDepartmentId);
        
        // 構建顯示名稱：學生姓名 - 關係描述
        String studentName = relation.getStudentName() != null ? relation.getStudentName() : "未知";
        String relationDesc = relation.getRelationDesc() != null ? relation.getRelationDesc() : "";
        node.setName(studentName + (relationDesc.isEmpty() ? "" : "-" + relationDesc));
        
        node.setRelationDesc(relationDesc);
        node.setMobile(relation.getMobile());
        node.setIsLeaf(true);
        return node;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

        // 1. 獲取部門數據
        if (departmentJson != null && departmentJson.getInteger("errcode") != null && departmentJson.getInteger("errcode") == 0) {
            JSONArray departmentsArray = departmentJson.getJSONArray("departments");
            // 2. 批量保存部門數據
            if (departmentsArray != null && !departmentsArray.isEmpty()) {
                List<SysDepartment> departmentsToSave = new ArrayList<>();
                // 遍歷部門數據
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
                // 批量保存部門數據
                batchSaveDepartments(departmentsToSave);
                // 3. 批量保存部門管理員數據
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

                // 批量保存部門管理員信息
                if (!allAdmins.isEmpty()) {
                    departmentAdminService.batchSaveDepartmentAdmins(allAdmins);
                }
            }
        }
    }
}
