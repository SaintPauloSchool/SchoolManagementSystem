package com.sms.system.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.WecomSchoolDepartment;
import com.sms.system.entity.WecomSchoolDepartmentMember;
import com.sms.system.mapper.WecomSchoolDepartmentMapper;
import com.sms.system.mapper.WecomSchoolDepartmentMemberMapper;
import com.sms.system.service.IWecomSchoolDepartmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * wecom学校部门 Service 实现类
 *
 */
@Service
public class WecomSchoolDepartmentServiceImpl implements IWecomSchoolDepartmentService {

    private static final Logger logger = LoggerFactory.getLogger(WecomSchoolDepartmentServiceImpl.class);

    @Autowired
    private WecomSchoolDepartmentMapper schoolDepartmentMapper;

    @Autowired
    private WecomSchoolDepartmentMemberMapper schoolDepartmentMemberMapper;

    /**
     * 获取学校部门树形结构（带成员）
     */
    @Override
    public List<WecomSchoolDepartment> getWecomSchoolDepartmentTreeWithMembers() {
        List<WecomSchoolDepartment> rootNodes = buildDepartmentTree();
        loadMembersForDepartments(rootNodes);
        return rootNodes;
    }

    /**
     * 获取学校部门树形结构（仅部门，不含人员）
     */
    @Override
    public List<WecomSchoolDepartment> getWecomSchoolDepartmentTree() {
        return buildDepartmentTree();
    }

    /**
     * 构建部门树形结构
     */
    private List<WecomSchoolDepartment> buildDepartmentTree() {
        // 1. 查询所有部门数据
        List<WecomSchoolDepartment> allDepartments = schoolDepartmentMapper.selectAll();

        if (allDepartments == null || allDepartments.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 构建父子关系映射
        Map<Long, List<WecomSchoolDepartment>> childrenMap = buildChildrenMap(allDepartments);

        // 3. 找到根节点（parentId 为 null 或 0）
        List<WecomSchoolDepartment> rootNodes = getRootNodes(allDepartments);

        // 4. 递归构建树形结构
        buildTree(rootNodes, childrenMap);

        return rootNodes;
    }

    /**
     * 构建父子关系映射
     */
    private Map<Long, List<WecomSchoolDepartment>> buildChildrenMap(List<WecomSchoolDepartment> allDepartments) {
        return allDepartments.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        dept -> Optional.ofNullable(dept.getParentId()).orElse((Integer) 0).longValue()
                ));
    }

    /**
     * 获取根节点列表
     */
    private List<WecomSchoolDepartment> getRootNodes(List<WecomSchoolDepartment> allDepartments) {
        return allDepartments.stream()
                .filter(dept -> Optional.ofNullable(dept.getParentId()).orElse((Integer) 0) == 0)
                .collect(Collectors.toList());
    }

    /**
     * 递归构建树形结构
     */
    private void buildTree(List<WecomSchoolDepartment> nodes, Map<Long, List<WecomSchoolDepartment>> childrenMap) {
        nodes.stream()
                .filter(Objects::nonNull)
                .filter(node -> node.getId() != null)
                .forEach(node -> {
                    List<WecomSchoolDepartment> children = childrenMap.get(node.getId());
                    if (children != null && !children.isEmpty()) {
                        node.setChildren(children);
                        buildTree(children, childrenMap);
                    }
                });
    }

    /**
     * 为部门加载成员数据
     */
    private void loadMembersForDepartments(List<WecomSchoolDepartment> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        // 收集所有需要查询成员的部门 ID
        List<Long> departmentIds = nodes.stream()
                .filter(this::isValidNonLeafDepartment)
                .map(WecomSchoolDepartment::getId)
                .distinct()
                .collect(Collectors.toList());

        if (departmentIds.isEmpty()) {
            return;
        }

        // 批量查询所有部门的成员并按部门 ID 分组
        Map<Long, List<WecomSchoolDepartmentMember>> membersMap = queryMembersByDepartmentIds(departmentIds);

        // 为每个部门分配成员数据并递归处理子部门
        nodes.stream()
                .filter(Objects::nonNull)
                .filter(dept -> dept.getId() != null)
                .filter(dept -> !Boolean.TRUE.equals(dept.getIsLeaf()))
                .forEach(dept -> processDepartmentMembers(dept, membersMap));
    }

    /**
     * 处理部门成员数据
     */
    private void processDepartmentMembers(WecomSchoolDepartment dept, Map<Long, List<WecomSchoolDepartmentMember>> membersMap) {
        // 获取部门成员
        List<WecomSchoolDepartmentMember> members = membersMap.get(dept.getId());

        if (members == null || members.isEmpty()) {
            return;
        }

        // 转换成员为节点并添加到 children
        List<WecomSchoolDepartment> memberNodes = members.stream()
                .filter(Objects::nonNull)
                .map(member -> convertToMemberNode(member, dept.getId()))
                .collect(Collectors.toList());

        if (!memberNodes.isEmpty()) {
            if (dept.getChildren() == null) {
                dept.setChildren(new ArrayList<>(memberNodes));
            } else {
                dept.getChildren().addAll(memberNodes);
            }
        }

        // 递归处理子部门
        if (dept.getChildren() != null) {
            List<WecomSchoolDepartment> childDepartments = dept.getChildren().stream()
                    .filter(child -> !Boolean.TRUE.equals(child.getIsLeaf()))
                    .collect(Collectors.toList());

            if (!childDepartments.isEmpty()) {
                loadMembersForDepartments(childDepartments);
            }
        }
    }

    /**
     * 判断是否为有效的非叶子部门节点
     */
    private boolean isValidNonLeafDepartment(WecomSchoolDepartment dept) {
        return dept != null
                && dept.getId() != null
                && !Boolean.TRUE.equals(dept.getIsLeaf());
    }

    /**
     * 批量查询部门成员
     */
    private Map<Long, List<WecomSchoolDepartmentMember>> queryMembersByDepartmentIds(List<Long> departmentIds) {
        // 查询所有部门成员
        List<WecomSchoolDepartmentMember> allMembers = schoolDepartmentMemberMapper.selectMembersByDepartmentIds(departmentIds);
        // 按部门 ID 分组
        return allMembers.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(WecomSchoolDepartmentMember::getDepartmentId));
    }

    /**
     * 将成员转换为部门节点（用于树形展示）
     */
    private WecomSchoolDepartment convertToMemberNode(WecomSchoolDepartmentMember member, Long currentDepartmentId) {
        WecomSchoolDepartment node = new WecomSchoolDepartment();
        node.setId(member.getId());
        node.setName(member.getName());
        node.setParentId(currentDepartmentId.intValue());
        node.setIsLeaf(true);
        node.setStaffUserId(member.getUserid());
        return node;
    }

    /**
     * 遞歸獲取 WeCom 部門及其所有子孫部門的 ID
     *
     * @param departmentIds 部門 ID 列表
     * @return 所有部門 ID 列表（包括傳入的部門及其所有子孫部門）
     */
    public List<Long> resolveAllDescendantDepartmentIds(List<Long> departmentIds) {
        // 如果傳入的部門 ID 列表為空，則返回空列表
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 查詢所有 WeCom 部門信息
        List<WecomSchoolDepartment> allDepartments = schoolDepartmentMapper.selectAll();

        // 2. 對每個傳入的部門 ID，遞歸查找其所有子孫部門
        Set<Long> allDepartmentIds = new HashSet<>(departmentIds);
        for (Long deptId : departmentIds) {
            collectAllDescendantDepartmentIds(deptId, allDepartments, allDepartmentIds);
        }

        return new ArrayList<>(allDepartmentIds);
    }

    /**
     * 遞歸收集 WeCom 某個部門的所有子孫部門 ID
     *
     * @param parentId 父部門 ID
     * @param allDepartments 所有部門列表
     * @param allDepartmentIds 收集結果的集合
     */
    private void collectAllDescendantDepartmentIds(Long parentId, List<WecomSchoolDepartment> allDepartments, Set<Long> allDepartmentIds) {
        if (parentId == null || allDepartments == null) {
            return;
        }

        // 找到所有直接子部門
        List<WecomSchoolDepartment> children = allDepartments.stream()
                .filter(Objects::nonNull)
                .filter(dept -> dept.getParentId() != null)
                .filter(dept -> dept.getParentId().longValue() == parentId)
                .collect(Collectors.toList());

        for (WecomSchoolDepartment child : children) {
            if (child.getId() == null) {
                continue;
            }

            // 添加子部門 ID
            allDepartmentIds.add(child.getId());
            
            // 繼續遞歸查找孫部門
            collectAllDescendantDepartmentIds(child.getId(), allDepartments, allDepartmentIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncWecomSchoolDepartments(JSONObject result) {
        if (result == null || result.getInteger("errcode") == null || result.getInteger("errcode") != 0) {
            logger.error("獲取部門列表失敗：{}", result != null ? result.getString("errmsg") : "返回結果為空");
            return;
        }

        JSONArray departmentArray = result.getJSONArray("department");
        if (departmentArray != null && !departmentArray.isEmpty()) {
            List<WecomSchoolDepartment> existingDepts = schoolDepartmentMapper.selectAll();
            Set<Long> existingIds = existingDepts.stream().map(WecomSchoolDepartment::getId).collect(Collectors.toSet());
            
            List<WecomSchoolDepartment> toInsert = new ArrayList<>();
            List<WecomSchoolDepartment> toUpdate = new ArrayList<>();
            
            for (int i = 0; i < departmentArray.size(); i++) {
                JSONObject deptObj = departmentArray.getJSONObject(i);
                WecomSchoolDepartment dept = new WecomSchoolDepartment();
                dept.setId(deptObj.getLong("id"));
                dept.setParentId(deptObj.getInteger("parentid"));
                dept.setName(deptObj.getString("name"));
                dept.setNameEn(deptObj.getString("name_en"));
                dept.setOrderNum(deptObj.getInteger("order"));
                JSONArray leaders = deptObj.getJSONArray("department_leader");
                if (leaders != null && !leaders.isEmpty()) {
                    dept.setDepartmentLeader(leaders.toJSONString());
                }
                dept.setUpdateTime(LocalDateTime.now());
                
                existingIds.remove(dept.getId());
                if (existingDepts.stream().anyMatch(d -> d.getId().equals(dept.getId()))) {
                    toUpdate.add(dept);
                } else {
                    dept.setCreateTime(LocalDateTime.now());
                    toInsert.add(dept);
                }
            }

            if (!toInsert.isEmpty()) schoolDepartmentMapper.batchInsertSchoolDepartments(toInsert);
            for (WecomSchoolDepartment dept : toUpdate) schoolDepartmentMapper.updateSchoolDepartment(dept);
            for (Long id : existingIds) schoolDepartmentMapper.deleteSchoolDepartmentById(id);

            logger.info("成功同步企業微信部門數據");
        } else {
            logger.warn("未獲取到企業微信部門數據");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncWecomSchoolDepartmentMembersBatch(Map<Long, JSONObject> departmentMembersMap) {
        if (departmentMembersMap == null || departmentMembersMap.isEmpty()) {
            return;
        }

        List<Long> departmentIds = new ArrayList<>(departmentMembersMap.keySet());

        // 1. 一次性查詢所有部門在資料庫中的現有成員 (1 次 SELECT SQL)
        List<WecomSchoolDepartmentMember> allDbMembers = schoolDepartmentMemberMapper.selectMembersByDepartmentIds(departmentIds);
        if (allDbMembers == null) {
            allDbMembers = new ArrayList<>();
        }

        // 按 departmentId 將現有成員分組
        Map<Long, List<WecomSchoolDepartmentMember>> dbMembersByDept = allDbMembers.stream()
                .collect(Collectors.groupingBy(WecomSchoolDepartmentMember::getDepartmentId));

        List<WecomSchoolDepartmentMember> allToSave = new ArrayList<>();
        List<Long> allIdsToDelete = new ArrayList<>();

        // 2. 遍歷處理每個部門的成員數據 (記憶體比對，無資料庫交互)
        for (Map.Entry<Long, JSONObject> entry : departmentMembersMap.entrySet()) {
            Long departmentId = entry.getKey();
            JSONObject memberResult = entry.getValue();

            if (memberResult == null || memberResult.getInteger("errcode") == null || memberResult.getInteger("errcode") != 0) {
                continue;
            }

            JSONArray userArray = memberResult.getJSONArray("userlist");
            List<WecomSchoolDepartmentMember> dbMembers = dbMembersByDept.getOrDefault(departmentId, Collections.emptyList());

            // 將該部門資料庫成員按 userid 分組
            Map<String, WecomSchoolDepartmentMember> dbMemberMap = dbMembers.stream()
                    .collect(Collectors.toMap(WecomSchoolDepartmentMember::getUserid, m -> m, (a, b) -> a));

            Set<String> incomingUserids = new HashSet<>();

            if (userArray != null && !userArray.isEmpty()) {
                for (int j = 0; j < userArray.size(); j++) {
                    JSONObject userObj = userArray.getJSONObject(j);
                    String userid = userObj.getString("userid");
                    incomingUserids.add(userid);

                    WecomSchoolDepartmentMember member = new WecomSchoolDepartmentMember();
                    member.setUserid(userid);
                    member.setName(userObj.getString("name"));
                    member.setDepartmentId(departmentId);
                    member.setOpenUserid(userObj.getString("open_userid"));
                    member.setUpdateTime(LocalDateTime.now());

                    if (dbMemberMap.containsKey(userid)) {
                        WecomSchoolDepartmentMember existingMember = dbMemberMap.get(userid);
                        member.setId(existingMember.getId());
                        member.setCreateTime(existingMember.getCreateTime());
                    } else {
                        member.setCreateTime(LocalDateTime.now());
                    }
                    allToSave.add(member);
                }
            }

            // 收集需要刪除的成員 ID
            for (WecomSchoolDepartmentMember dbMember : dbMembers) {
                if (!incomingUserids.contains(dbMember.getUserid())) {
                    allIdsToDelete.add(dbMember.getId());
                }
            }
        }

        // 3. 一次性批次執行刪除 (1 次 DELETE SQL)
        if (!allIdsToDelete.isEmpty()) {
            schoolDepartmentMemberMapper.deleteMembersByIds(allIdsToDelete);
            logger.info("批次同步成員：增量刪除 {} 個已不存在的部門成員", allIdsToDelete.size());
        }

        // 4. 一次性批次執行插入與更新 (1 次 INSERT SQL)
        if (!allToSave.isEmpty()) {
            schoolDepartmentMemberMapper.batchInsertSchoolDepartmentMembers(allToSave);
            logger.info("批次同步成員：新增或更新 {} 個部門成員", allToSave.size());
        }
    }
}
