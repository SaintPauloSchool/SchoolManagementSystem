package com.sms.system.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.common.config.StudentProfilesProperties;
import com.sms.common.utils.bean.BeanCopyUtils;
import com.sms.system.entity.SysDepartment;
import com.sms.system.entity.SysDepartmentAdmin;
import com.sms.system.entity.vo.SysDepartmentVO;
import com.sms.system.entity.vo.SysSchoolFamilyContactVO;
import com.sms.system.mapper.SysDepartmentAdminMapper;
import com.sms.system.mapper.SysDepartmentMapper;
import com.sms.system.mapper.SysSchoolFamilyContactMapper;
import com.sms.system.service.ISysConfigService;
import com.sms.system.service.ISysDepartmentAdminService;
import com.sms.system.service.ISysDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private SysSchoolFamilyContactMapper schoolFamilyContactMapper;

    @Autowired
    private SysDepartmentAdminMapper departmentAdminMapper;

    @Autowired
    private ISysConfigService sysConfigService;

    @Autowired
    private StudentProfilesProperties studentProfilesProperties;

    /**
     * 獲取家校通訊錄樹（帶家長學生關係）。
     * <p>根據 sys_config 配置的學段部門，構建 學段→年級→班級 樹；
     * 班級下掛載已匹配學籍的家長聯絡人。</p>
     *
     * @param openUserId 企業微信 userid
     * @return 過濾後的帶家長學生關係樹形結構
     */
    @Override
    public List<SysDepartmentVO> getClassTreeWithParentsByAdmin(String openUserId) {
        // 1. 獲取當前用戶可管理的部門 ID（sys_department_admin）
        Set<Long> adminDeptIds = getAdminDepartmentIds(openUserId);
        if (adminDeptIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 讀取基本設置中配置的學段部門（type=3）
        Long segmentDepartmentId = sysConfigService.getAddressBookSegmentDepartmentId();
        if (segmentDepartmentId == null) {
            return Collections.emptyList();
        }

        // 3. 以學段為根，構建 學段→年級→班級 部門樹
        List<SysDepartmentVO> addressBookTree = getAddressBookDeptTree(segmentDepartmentId);
        if (addressBookTree.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. 為班級節點掛載已匹配學籍的家長聯絡人（class_section → student_info → sys_student_match → sys_school_family_contact）
        loadMatchedAddressBookContacts(addressBookTree);

        // 5. 按管理員權限剪枝後返回
        return filterTree(addressBookTree, adminDeptIds);
    }

    /**
     * 查詢當前用戶在企微家校通訊錄中可管理的部門 ID 集合。
     *
     * @param openUserId 企業微信 userid（當前登錄用戶）
     * @return 可管理部門 ID 集合；無記錄時返回空集合
     */
    private Set<Long> getAdminDepartmentIds(String openUserId) {
        if (openUserId == null || openUserId.isEmpty()) {
            return Collections.emptySet();
        }
        // 查詢該用戶在 sys_department_admin 中的管理記錄
        List<SysDepartmentAdmin> adminRecords = departmentAdminMapper.selectByUserid(openUserId);
        if (adminRecords == null || adminRecords.isEmpty()) {
            return Collections.emptySet();
        }
        // 提取部門 ID（可能含學段、年級、班級等不同層級）
        Set<Long> ids = new HashSet<>();
        for (SysDepartmentAdmin record : adminRecords) {
            if (record.getDepartmentId() != null) {
                ids.add(record.getDepartmentId());
            }
        }
        return ids;
    }

    /**
     * 根據配置的學段部門 ID，構建 學段(type=3) → 年級(2) → 班級(1) 樹。
     */
    private List<SysDepartmentVO> getAddressBookDeptTree(Long segmentDepartmentId) {
        // 查詢全部部門（順序沿用 SQL：type ASC, order_num ASC）
        List<SysDepartment> allDepartments = departmentMapper.selectAll();
        if (allDepartments == null || allDepartments.isEmpty()) {
            return Collections.emptyList();
        }

        // 定位配置的學段節點，必須為 type=3
        SysDepartment segment = allDepartments.stream()
                .filter(dept -> dept != null && segmentDepartmentId != null
                        && segmentDepartmentId.equals(dept.getId()))
                .findFirst()
                .orElse(null);
        if (segment == null || segment.getType() == null || segment.getType() != TYPE_SCHOOL_SEGMENT) {
            return Collections.emptyList();
        }

        // 以學段為根，向下掛載年級、班級子節點
        List<SysDepartmentVO> rootNodes = Collections.singletonList(toDeptVo(segment));
        // 從學段向下構建部門樹（僅到 type=1 班級），順序沿用 SQL 查詢結果。
        buildAddressBookDeptTree(rootNodes, allDepartments, TYPE_SCHOOL_SEGMENT);
        // 響應數據
        return rootNodes;
    }

    /**
     * 從學段向下遞歸構建部門樹（僅到 type=1 班級），順序沿用 SQL 查詢結果。
     *
     * @param currentLevel   當前層級節點（首次調用為學段根節點）
     * @param allDepartments 全部部門扁平列表（來自 selectAll）
     * @param currentType    當前層級 type（首次為 3 學段，遞歸時依次為 2 年級）
     */
    private void buildAddressBookDeptTree(List<SysDepartmentVO> currentLevel,
                                          List<SysDepartment> allDepartments,
                                          int currentType) {
        if (currentLevel == null || currentLevel.isEmpty()) {
            return;
        }

        // 下一層 type 遞減：學段(3) → 年級(2) → 班級(1)
        int nextType = currentType - 1;
        if (nextType < TYPE_CLASS) {
            return;
        }

        for (SysDepartmentVO currentDept : currentLevel) {
            if (currentDept == null || currentDept.getId() == null) {
                continue;
            }

            long currentId = currentDept.getId();
            // 從全量列表中篩選 parent_id 指向當前節點的直接子部門
            List<SysDepartmentVO> children = new ArrayList<>();
            for (SysDepartment dept : allDepartments) {
                if (dept == null || dept.getType() == null || dept.getType() != nextType) {
                    continue;
                }
                if (dept.getParentId() != null && dept.getParentId().longValue() == currentId) {
                    children.add(toDeptVo(dept));
                }
            }

            if (!children.isEmpty()) {
                // 年級下的班級節點：先排全部學生部門，再排全部家長部門
                if (nextType == TYPE_CLASS) {
                    children.sort(this::compareClassDepartmentByRoleThenName);
                }
                currentDept.setChildren(children);
                // 班級為葉子節點，不再向下遞歸
                if (nextType > TYPE_CLASS) {
                    buildAddressBookDeptTree(children, allDepartments, nextType);
                }
            }
        }
    }

    /** 將部門實體轉為 VO（僅含 sys_department 表字段） */
    private SysDepartmentVO toDeptVo(SysDepartment dept) {
        return BeanCopyUtils.copy(dept, SysDepartmentVO.class);
    }

    /**
     * 班級部門排序：學生部門在前、家長部門在後，同組內按名稱升序。
     */
    private int compareClassDepartmentByRoleThenName(SysDepartmentVO a, SysDepartmentVO b) {
        int roleCompare = Integer.compare(
                classDepartmentRoleRank(a != null ? a.getName() : null),
                classDepartmentRoleRank(b != null ? b.getName() : null)
        );
        if (roleCompare != 0) {
            return roleCompare;
        }
        String nameA = a != null && a.getName() != null ? a.getName() : "";
        String nameB = b != null && b.getName() != null ? b.getName() : "";
        return nameA.compareTo(nameB);
    }

    private int classDepartmentRoleRank(String name) {
        if (name == null) {
            return 2;
        }
        if (name.endsWith("_學生")) {
            return 0;
        }
        if (name.endsWith("_家長")) {
            return 1;
        }
        return 2;
    }

    /**
     * 為班級節點加載已匹配學籍的家長聯絡人。
     * <p>關聯：部門 name → class_section → student_info → sys_student_match → sys_school_family_contact</p>
     */
    private void loadMatchedAddressBookContacts(List<SysDepartmentVO> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        // 1. 遞歸收集樹中所有班級節點（type=1）
        List<SysDepartmentVO> classNodes = new ArrayList<>();
        collectClassNodes(nodes, classNodes);
        if (classNodes.isEmpty()) {
            return;
        }

        // 2. 提取班級部門 ID，批量查詢（避免 N+1）
        List<Long> classIds = classNodes.stream()
                .map(SysDepartmentVO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (classIds.isEmpty()) {
            return;
        }

        // 3. 按關聯鏈查詢已匹配學籍的家長聯絡人
        List<SysSchoolFamilyContactVO> allRelations = schoolFamilyContactMapper
                .selectMatchedContactsByDepartmentIds(
                        classIds,
                        studentProfilesProperties.getDatabase()
                );
        if (allRelations == null || allRelations.isEmpty()) {
            return;
        }

        // 4. 按班級部門 ID 分組，便於掛載到對應班級節點下
        Map<Long, List<SysSchoolFamilyContactVO>> relationsByClassId = allRelations.stream()
                .filter(Objects::nonNull)
                .filter(relation -> relation.getDepartmentId() != null)
                .collect(Collectors.groupingBy(SysSchoolFamilyContactVO::getDepartmentId));

        // 5. 將家長聯絡人轉為葉子節點，掛到各班級下
        for (SysDepartmentVO classNode : classNodes) {
            List<SysSchoolFamilyContactVO> relations = relationsByClassId.get(classNode.getId());
            if (relations == null || relations.isEmpty()) {
                continue;
            }

            if (classNode.getChildren() == null) {
                classNode.setChildren(new ArrayList<>());
            }

            for (SysSchoolFamilyContactVO relation : relations) {
                classNode.getChildren().add(
                        //  將家長學生關係轉換爲葉子 VO 節點
                        convertToLeafVo(
                                relation,
                                classNode.getId()
                        )
                );
            }
        }
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
    private List<SysDepartmentVO> filterTree(List<SysDepartmentVO> nodes, Set<Long> adminIds) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysDepartmentVO> result = new ArrayList<>();
        for (SysDepartmentVO node : nodes) {
            if (node == null) {
                continue;
            }

            if (adminIds.contains(node.getId())) {
                // 當前節點有權限，直接保留（子節點全部保留，無需繼續過濾）
                result.add(node);
            } else {
                // 遞歸過濾子節點
                List<SysDepartmentVO> filteredChildren = filterTree(node.getChildren(), adminIds);
                if (!filteredChildren.isEmpty()) {
                    // 有子節點保留，則當前節點也保留（作爲路徑節點），並替換子節點列表
                    SysDepartmentVO copy = shallowCopyDeptVo(node);
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
    private SysDepartmentVO shallowCopyDeptVo(SysDepartmentVO src) {
        SysDepartmentVO copy = new SysDepartmentVO();
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
        return copy;
    }

    /**
     * 遞歸收集所有班級節點（type=1）
     *
     * @param nodes      部門節點列表
     * @param classNodes 用於存儲收集到的班級節點
     */
    private void collectClassNodes(List<SysDepartmentVO> nodes, List<SysDepartmentVO> classNodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        for (SysDepartmentVO dept : nodes) {
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
     * 將家長學生關係轉換爲葉子 VO 節點
     * 節點名稱格式：學生姓名 - 關係描述（如：張三 - 父親）
     *
     * @param relation          家長學生關係
     * @param classDepartmentId 所屬家長班級部門 ID
     * @return 葉子 VO 節點
     */
    private SysDepartmentVO convertToLeafVo(SysSchoolFamilyContactVO relation, Long classDepartmentId) {
        SysDepartmentVO node = new SysDepartmentVO();
        node.setId(relation.getId());
        node.setStudentUserId(relation.getStudentUserId());
        node.setStudentId(relation.getStudentId());
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

    // =========================================================================
    // 同步家校通訊錄使用
    // =========================================================================

    /**
     * 查詢家校通訊錄中所有班級部門 ID（type=1）。
     *
     * @return 班級部門 ID 列表，無符合條件時返回空列表
     */
    @Override
    public List<Long> getClassDepartmentId() {
        return departmentMapper.selectClassDepartmentId();
    }

    // =========================================================================
    // 同步部門數據使用
    // =========================================================================

    /**
     * 同步學校部門數據
     *
     * @param departmentJson 微信接口返回的部門 JSON 數據
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncSchoolDepartmentData(JSONObject departmentJson) {
        // 1. 獲取部門數據
        if (departmentJson != null
                && departmentJson.getInteger("errcode") != null
                && departmentJson.getInteger("errcode") == 0) {

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


    /**
     * 批量寫入或更新家校通訊錄部門數據。
     *
     * @param departments 待保存的部門列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchSaveDepartments(List<SysDepartment> departments) {
        if (departments != null && !departments.isEmpty()) {
            departmentMapper.batchInsertDepartments(departments);
        }
    }

    // =========================================================================
    // 系統基礎設置使用
    // =========================================================================

    /**
     * 獲取學段樹，供基本設置頁選擇家校通訊錄使用的學段（type=3）。
     */
    @Override
    public List<SysDepartmentVO> getSegmentTree() {
        return getSegmentTreeInternal();
    }

    /**
     * 從扁平部門列表構建學段樹。
     * <p>層級：type 5(學校) → type 4(校區) → type 3(學段)，不含年級/班級。
     * 順序沿用 {@link SysDepartmentMapper#selectAll()} 的 SQL 排序結果。</p>
     */
    private List<SysDepartmentVO> getSegmentTreeInternal() {
        // 獲取部門數據
        List<SysDepartment> allDepartments = departmentMapper.selectAll();
        if (allDepartments == null || allDepartments.isEmpty()) {
            return Collections.emptyList();
        }

        // 優先以學校為根；若無學校節點則退而從校區開始
        List<SysDepartmentVO> rootNodes = collectDepartmentsByType(allDepartments, TYPE_SCHOOL).stream()
                .map(this::toDeptVo)
                .collect(Collectors.toList());
        if (rootNodes.isEmpty()) {
            rootNodes = collectDepartmentsByType(allDepartments, TYPE_CAMPUS).stream()
                    .map(this::toDeptVo)
                    .collect(Collectors.toList());
            if (!rootNodes.isEmpty()) {
                buildSegmentTree(rootNodes, allDepartments, TYPE_CAMPUS);
                return rootNodes;
            }
            return Collections.emptyList();
        }

        buildSegmentTree(rootNodes, allDepartments, TYPE_SCHOOL);
        return rootNodes;
    }

    /**
     * 按 SQL 查詢順序收集指定類型的部門（保持 selectAll 返回的相對順序）。
     */
    private List<SysDepartment> collectDepartmentsByType(List<SysDepartment> allDepartments, int type) {
        List<SysDepartment> result = new ArrayList<>();
        for (SysDepartment dept : allDepartments) {
            if (dept != null && dept.getType() != null && dept.getType() == type) {
                result.add(dept);
            }
        }
        return result;
    }

    /**
     * 遞歸掛載子節點，構建學段樹。
     * <p>根據 parent_id 關聯父子，僅展開到 type=3；type=2/1 不會掛載。
     * 子節點順序與 allDepartments 中的出現順序一致。</p>
     *
     * @param currentLevel   當前層節點列表
     * @param allDepartments 全部部門（已按 SQL 排序）
     * @param currentType    當前層類型（5=學校，4=校區）
     */
    private void buildSegmentTree(List<SysDepartmentVO> currentLevel,
                                  List<SysDepartment> allDepartments,
                                  Integer currentType) {
        if (currentLevel == null || currentLevel.isEmpty()) {
            return;
        }

        // 下一層類型：學校(5)→校區(4)→學段(3)
        Integer nextType = currentType - 1;
        if (nextType < TYPE_SCHOOL_SEGMENT) {
            return;
        }

        for (SysDepartmentVO currentDept : currentLevel) {
            if (currentDept == null || currentDept.getId() == null) {
                continue;
            }

            long currentId = currentDept.getId();
            // 遍歷全表，按 SQL 順序收集直接子節點
            List<SysDepartmentVO> children = new ArrayList<>();
            for (SysDepartment dept : allDepartments) {
                if (dept == null || dept.getType() == null || !dept.getType().equals(nextType)) {
                    continue;
                }
                if (dept.getParentId() != null && dept.getParentId().longValue() == currentId) {
                    children.add(toDeptVo(dept));
                }
            }

            if (!children.isEmpty()) {
                currentDept.setChildren(children);
                // 校區下還有學段需繼續展開；學段已是葉節點
                if (nextType > TYPE_SCHOOL_SEGMENT) {
                    buildSegmentTree(children, allDepartments, nextType);
                }
            }
        }
    }
}
