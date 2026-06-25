package com.sms.system.service.impl.notification;

import com.sms.common.exception.ServiceException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.SysDepartment;
import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.entity.SysParentStudentRelation;
import com.sms.system.entity.SysSchoolDepartmentMember;
import com.sms.system.entity.dto.NotificationReceiverSaveDTO;
import com.sms.system.entity.notification.NotificationReceiver;
import com.sms.system.entity.notification.receiver.ReceiverResolutionContext;
import com.sms.system.entity.vo.NotificationReceiverVO;
import com.sms.system.entity.vo.ResolvedReceiversVO;
import com.sms.system.mapper.SysDepartmentMapper;
import com.sms.system.mapper.SysDepartmentParentBindingMapper;
import com.sms.system.mapper.SysParentStudentRelationMapper;
import com.sms.system.mapper.SysSchoolDepartmentMemberMapper;
import com.sms.system.mapper.notification.NotificationReceiverMapper;
import com.sms.system.service.impl.SysSchoolDepartmentServiceImpl;
import com.sms.system.service.notification.INotificationReceiverService;
import com.sms.common.utils.bean.BeanCopyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * 通知接收對象 Service。
 * <p>
 * 負責保存前端選擇的接收人配置，並在發佈時將 {@code receive_data} 解析為企微可發送的數據：
 * 家長 userid 列表、家長-學生-班級綁定、部門映射等。
 * </p>
 * <p>解析維度說明：</p>
 * <ul>
 *   <li>{@code receiveType}：1=按班級/部門，2=按個人</li>
 *   <li>{@code data.type}：1=企微家校通訊錄，2=自定義家校通訊錄</li>
 * </ul>
 */
@Service
public class NotificationReceiverServiceImpl implements INotificationReceiverService {

    private static final Logger log = LoggerFactory.getLogger(NotificationReceiverServiceImpl.class);

    /** receiveType=1：按部門發佈（選班級或自定義部門） */
    private static final String RECEIVE_TYPE_DEPARTMENT = "1";
    /** receiveType=2：按個人發佈（選具體家長/成員） */
    private static final String RECEIVE_TYPE_PERSONAL = "2";
    /** data.type=1：數據來自企微家校通訊錄 */
    private static final Integer TARGET_TYPE_WECOM = 1;
    /** data.type=2：數據來自自定義家校通訊錄（sys_school_department_member） */
    private static final Integer TARGET_TYPE_CUSTOM = 2;

    @Autowired
    private NotificationReceiverMapper notificationReceiverMapper;

    @Autowired
    private SysParentStudentRelationMapper parentStudentRelationMapper;

    @Autowired
    private SysSchoolDepartmentMemberMapper schoolDepartmentMemberMapper;

    @Autowired
    private SysDepartmentParentBindingMapper departmentParentBindingMapper;

    @Autowired
    private SysDepartmentMapper sysDepartmentMapper;

    @Autowired
    private SysSchoolDepartmentServiceImpl sysSchoolDepartmentService;

    // -------------------------------------------------------------------------
    // 公開 API：查詢與保存
    // -------------------------------------------------------------------------

    /** 按通知 ID 查詢接收對象配置（用於詳情展示、撤回時重新解析） */
    @Override
    public List<NotificationReceiverVO> selectByNotificationId(Long notificationId) {
        return BeanCopyUtils.copyList(notificationReceiverMapper.selectByNotificationId(notificationId),
                NotificationReceiverVO.class);
    }

    /** 保存單條接收對象配置到 notification_receiver 表 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int save(NotificationReceiverSaveDTO notificationReceiverSaveDTO) {
        NotificationReceiver receiver = BeanCopyUtils.copy(notificationReceiverSaveDTO, NotificationReceiver.class);
        if (receiver.getCreateTime() == null) {
            receiver.setCreateTime(LocalDateTime.now());
        }
        return notificationReceiverMapper.insert(receiver);
    }

    // -------------------------------------------------------------------------
    // 核心：解析接收人
    // -------------------------------------------------------------------------

    /**
     * 將通知的接收對象配置解析為發送所需的結構化數據。
     * <p>輸出 {@link ResolvedReceiversVO}，主要包含：</p>
     * <ul>
     *   <li>{@code parentUserIds}：實際發送目標（企微家長 userid）</li>
     *   <li>{@code bindings}：企微選人時的家長-學生-班級三方綁定（用於個性化消息與閱讀記錄）</li>
     *   <li>{@code parentStudentUserIds}：自定義家校成員的家長→學生映射（來自成員表 student_user_id，發佈時校驗不可缺失）</li>
     *   <li>{@code studentDepartmentIds}：家長 userid → 部門 ID</li>
     * </ul>
     *
     * @param strictDepartmentCheck true=發佈時校驗；一家長多班級且未指定班級則拋錯。false=撤回等場景放寬
     */
    @Override
    public ResolvedReceiversVO resolveReceivers(List<NotificationReceiver> receivers, boolean strictDepartmentCheck) {
        ReceiverResolutionContext context = new ReceiverResolutionContext();

        if (receivers == null || receivers.isEmpty()) {
            log.warn("notification receivers are empty");
            return context.toResult();
        }

        try {
            for (NotificationReceiver receiver : receivers) {
                parseReceiver(receiver, context, strictDepartmentCheck);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("failed to resolve notification receivers", e);
            throw new ServiceException("解析接收對象失敗：" + e.getMessage());
        }

        // 自定義家校成員必須在成員表存有 student_user_id，不可用關係表猜測（一家長多學生會猜錯）
        validateCustomMemberStudentUserId(context, strictDepartmentCheck);
        List<SysDepartmentParentBinding> uniqueBindings = deduplicateBindings(context.bindings);

        log.info("解析完成 - parentUserIds: {}, studentUserIds: {}, 綁定關係數: {} (去重前: {})",
                context.parentUserIds.size(), context.studentUserIds.size(),
                uniqueBindings.size(), context.bindings.size());

        return context.toResult(uniqueBindings);
    }

    // -------------------------------------------------------------------------
    // receive_data 解析入口
    // -------------------------------------------------------------------------

    /** 解析單條 notification_receiver 記錄（receiveType + receive_data JSON 陣列） */
    private void parseReceiver(NotificationReceiver receiver, ReceiverResolutionContext context,
                               boolean strictDepartmentCheck) {
        String receiveData = receiver.getReceiveData();
        if (receiveData == null || receiveData.trim().isEmpty()) {
            return;
        }

        JSONArray dataArray = JSON.parseArray(receiveData);
        if (dataArray == null || dataArray.isEmpty()) {
            return;
        }

        for (int i = 0; i < dataArray.size(); i++) {
            parseDataItem(receiver.getReceiveType(), dataArray.getJSONObject(i), context, strictDepartmentCheck);
        }
    }

    /**
     * 解析 receive_data 中的單個分組（同一 type 的一批 ID）。
     * <p>例如：{@code {"type":1, "receive_ids":[...], "department_ids":[...]}}</p>
     */
    private void parseDataItem(String receiveType, JSONObject dataItem, ReceiverResolutionContext context,
                               boolean strictDepartmentCheck) {
        Integer type = dataItem.getInteger("type");
        JSONArray ids = dataItem.getJSONArray("receive_ids");
        if (ids == null || ids.isEmpty()) {
            ids = dataItem.getJSONArray("ids");
        }
        if (type == null || ids == null || ids.isEmpty()) {
            return;
        }

        List<Long> idList = ids.toJavaList(Long.class);
        List<Long> departmentIdList = buildDepartmentIdList(dataItem, idList);

        if (RECEIVE_TYPE_PERSONAL.equals(receiveType)) {
            resolvePersonalReceivers(type, idList, departmentIdList, context, strictDepartmentCheck);
        } else if (RECEIVE_TYPE_DEPARTMENT.equals(receiveType)) {
            resolveDepartmentReceivers(type, idList, context);
        }
    }

    /**
     * 解析與 receive_ids 對齊的 department_ids（按個人選人時標明所屬班級）。
     * 長度不一致時返回等長的 null 列表，表示未指定班級。
     */
    private List<Long> buildDepartmentIdList(JSONObject dataItem, List<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return Collections.emptyList();
        }
        JSONArray departmentIds = dataItem.getJSONArray("department_ids");
        if (departmentIds == null || departmentIds.size() != idList.size()) {
            return new ArrayList<>(Collections.nCopies(idList.size(), null));
        }
        List<Long> result = new ArrayList<>(idList.size());
        for (int i = 0; i < idList.size(); i++) {
            result.add(departmentIds.getLong(i));
        }
        return result;
    }

    /** 按個人維度分發到企微或自定義家校解析器 */
    private void resolvePersonalReceivers(Integer type, List<Long> idList, List<Long> departmentIdList,
                                          ReceiverResolutionContext context, boolean strictDepartmentCheck) {
        if (TARGET_TYPE_WECOM.equals(type)) {
            resolveWecomPersonalReceivers(idList, departmentIdList, context, strictDepartmentCheck);
        } else if (TARGET_TYPE_CUSTOM.equals(type)) {
            resolveCustomPersonalReceivers(idList, departmentIdList, context);
        }
    }

    /** 按部門維度分發到企微或自定義家校解析器 */
    private void resolveDepartmentReceivers(Integer type, List<Long> idList, ReceiverResolutionContext context) {
        if (TARGET_TYPE_WECOM.equals(type)) {
            resolveWecomDepartmentReceivers(idList, context);
        } else if (TARGET_TYPE_CUSTOM.equals(type)) {
            resolveCustomDepartmentReceivers(idList, context);
        }
    }

    // -------------------------------------------------------------------------
    // 企微家校：按個人（receive_ids = sys_parent_student_relation.id）
    // -------------------------------------------------------------------------

    /**
     * 企微按個人選人：根據家長-學生關係 ID 查出家長 userid，並建立班級綁定。
     * <p>bindings 用於個性化消息（班級名+學生名）及閱讀記錄寫入。</p>
     */
    private void resolveWecomPersonalReceivers(List<Long> ids, List<Long> departmentIds,
                                               ReceiverResolutionContext context, boolean strictDepartmentCheck) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Long> uniqueRelationIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (uniqueRelationIds.isEmpty()) {
            return;
        }

        List<SysParentStudentRelation> relations = parentStudentRelationMapper.selectByIds(uniqueRelationIds);
        if (relations == null || relations.isEmpty()) {
            return;
        }

        Map<Long, SysParentStudentRelation> relationMap = relations.stream()
                .filter(relation -> relation.getId() != null)
                .collect(Collectors.toMap(SysParentStudentRelation::getId, relation -> relation, (a, b) -> a));

        Map<String, List<SysDepartmentParentBinding>> parentStudentBindingsMap =
                loadParentStudentBindingsMap(relations);

        for (int i = 0; i < ids.size(); i++) {
            Long relationId = ids.get(i);
            if (relationId == null) {
                continue;
            }
            SysParentStudentRelation relation = relationMap.get(relationId);
            if (relation == null || relation.getParentUserId() == null || relation.getParentUserId().trim().isEmpty()) {
                continue;
            }

            context.parentUserIds.add(relation.getParentUserId());
            String studentUserId = relation.getStudentUserId();
            Long explicitDepartmentId = i < departmentIds.size() ? departmentIds.get(i) : null;
            SysDepartmentParentBinding resolvedBinding = resolveParentBinding(
                    relation, explicitDepartmentId,
                    parentStudentBindingsMap.get(relation.getParentUserId() + "_" + studentUserId),
                    strictDepartmentCheck);
            Long departmentId = resolvedBinding != null ? resolvedBinding.getDepartmentId() : null;

            if (!tryAddBinding(context, relation.getParentUserId(), studentUserId, departmentId)) {
                continue;
            }

            if (resolvedBinding != null) {
                context.bindings.add(resolvedBinding);
            } else {
                SysDepartmentParentBinding binding = new SysDepartmentParentBinding();
                binding.setParentUserId(relation.getParentUserId());
                binding.setStudentUserId(studentUserId);
                binding.setDepartmentId(null);
                context.bindings.add(binding);
            }
        }
    }

    /** 批量加載家長-學生對應的班級綁定，key = parentUserId_studentUserId */
    private Map<String, List<SysDepartmentParentBinding>> loadParentStudentBindingsMap(
            List<SysParentStudentRelation> relations) {
        List<String> studentUserIds = relations.stream()
                .map(SysParentStudentRelation::getStudentUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<String, List<SysDepartmentParentBinding>> parentStudentBindingsMap = new HashMap<>();
        if (studentUserIds.isEmpty()) {
            return parentStudentBindingsMap;
        }

        List<SysDepartmentParentBinding> existingBindings =
                departmentParentBindingMapper.selectByStudentUserIds(studentUserIds);
        if (existingBindings == null) {
            return parentStudentBindingsMap;
        }

        for (SysDepartmentParentBinding binding : existingBindings) {
            if (binding.getStudentUserId() != null && binding.getParentUserId() != null) {
                String key = binding.getParentUserId() + "_" + binding.getStudentUserId();
                parentStudentBindingsMap.computeIfAbsent(key, k -> new ArrayList<>()).add(binding);
            }
        }
        return parentStudentBindingsMap;
    }

    /**
     * 為企微個人選人解析班級綁定：優先使用前端傳入的 department_id，其次唯一匹配，多匹配時依 strict 決定報錯或取第一筆。
     */
    private SysDepartmentParentBinding resolveParentBinding(SysParentStudentRelation relation, Long explicitDepartmentId,
                                                            List<SysDepartmentParentBinding> candidateBindings,
                                                            boolean strictDepartmentCheck) {
        if (explicitDepartmentId != null) {
            if (candidateBindings != null) {
                for (SysDepartmentParentBinding binding : candidateBindings) {
                    if (explicitDepartmentId.equals(binding.getDepartmentId())) {
                        return binding;
                    }
                }
            }
            SysDepartmentParentBinding binding = new SysDepartmentParentBinding();
            binding.setParentUserId(relation.getParentUserId());
            binding.setStudentUserId(relation.getStudentUserId());
            binding.setDepartmentId(explicitDepartmentId);
            return binding;
        }
        if (candidateBindings == null || candidateBindings.isEmpty()) {
            return null;
        }
        if (candidateBindings.size() == 1) {
            return candidateBindings.get(0);
        }
        if (!strictDepartmentCheck) {
            log.warn("家長學生關係存在多個部門綁定且未指定 department_id，已使用第一筆: parentUserId={}, studentUserId={}",
                    relation.getParentUserId(), relation.getStudentUserId());
            return candidateBindings.get(0);
        }
        throw new ServiceException(String.format(
                "家長「%s」存在多個班級綁定，請在選人時指定班級後再發送（relationId=%d）",
                relation.getStudentName() != null ? relation.getStudentName() : relation.getParentUserId(),
                relation.getId()));
    }

    // -------------------------------------------------------------------------
    // 自定義家校：按個人（receive_ids = sys_school_department_member.id）
    // -------------------------------------------------------------------------

    /**
     * 自定義家校按個人選人：member.userid 為家長企微 ID，走 to_parent_userid 發送。
     * <p>不產生 bindings，通過 parentStudentUserIds 記錄家長→學生映射供閱讀記錄使用。</p>
     */
    private void resolveCustomPersonalReceivers(List<Long> ids, List<Long> departmentIds,
                                                ReceiverResolutionContext context) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Long> uniqueMemberIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (uniqueMemberIds.isEmpty()) {
            return;
        }

        List<SysSchoolDepartmentMember> members = schoolDepartmentMemberMapper.selectMembersByIds(uniqueMemberIds);
        if (members == null || members.isEmpty()) {
            return;
        }

        Map<Long, SysSchoolDepartmentMember> memberMap = members.stream()
                .filter(member -> member.getId() != null)
                .collect(Collectors.toMap(SysSchoolDepartmentMember::getId, member -> member, (a, b) -> a));

        for (int i = 0; i < ids.size(); i++) {
            Long memberId = ids.get(i);
            if (memberId == null) {
                continue;
            }
            SysSchoolDepartmentMember member = memberMap.get(memberId);
            if (member == null || member.getUserid() == null || member.getUserid().trim().isEmpty()) {
                continue;
            }
            registerCustomMember(context, member, i < departmentIds.size() ? departmentIds.get(i) : null);
        }
    }

    // -------------------------------------------------------------------------
    // 企微家校：按部門（展開班級下所有家長綁定）
    // -------------------------------------------------------------------------

    /**
     * 企微按部門選人：遞歸找到 type=1 班級部門，查詢班級下所有家長-學生綁定。
     */
    private void resolveWecomDepartmentReceivers(List<Long> departmentIds, ReceiverResolutionContext context) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return;
        }

        List<Long> classDepartmentIds = resolveClassDepartmentIds(departmentIds);
        log.info("解析部門家長綁定 - 輸入部門 IDs: {}, 解析後班級部門 IDs: {}", departmentIds, classDepartmentIds);
        if (classDepartmentIds.isEmpty()) {
            return;
        }

        List<SysDepartmentParentBinding> bindings =
                departmentParentBindingMapper.selectByDepartmentIds(classDepartmentIds);
        if (bindings == null) {
            return;
        }

        for (SysDepartmentParentBinding binding : bindings) {
            if (binding.getParentUserId() == null || binding.getParentUserId().trim().isEmpty()) {
                continue;
            }
            context.parentUserIds.add(binding.getParentUserId());
            if (tryAddBinding(context, binding.getParentUserId(), binding.getStudentUserId(),
                    binding.getDepartmentId())) {
                context.bindings.add(binding);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 自定義家校：按部門（展開子部門下所有成員）
    // -------------------------------------------------------------------------

    /**
     * 自定義家校按部門選人：遞歸展開子部門，查詢部門下全部成員的家長 userid。
     */
    private void resolveCustomDepartmentReceivers(List<Long> departmentIds, ReceiverResolutionContext context) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return;
        }

        List<Long> allDescendantDepartmentIds =
                sysSchoolDepartmentService.resolveAllDescendantDepartmentIdsByType(departmentIds, 2);
        log.info("解析自定義家校部門成員 - 輸入部門 IDs: {}, 解析後所有子孫部門 IDs: {}",
                departmentIds, allDescendantDepartmentIds);
        if (allDescendantDepartmentIds.isEmpty()) {
            return;
        }

        List<SysSchoolDepartmentMember> members =
                schoolDepartmentMemberMapper.selectMembersByDepartmentIds(allDescendantDepartmentIds);
        if (members == null) {
            return;
        }

        for (SysSchoolDepartmentMember member : members) {
            if (member.getUserid() == null || member.getUserid().trim().isEmpty()) {
                continue;
            }
            registerCustomMember(context, member, null);
        }
    }

    /** 將自定義家校成員註冊到解析上下文：家長 userid、學生映射、部門 ID */
    private void registerCustomMember(ReceiverResolutionContext context, SysSchoolDepartmentMember member,
                                      Long explicitDepartmentId) {
        context.parentUserIds.add(member.getUserid());
        registerParentStudentMapping(context, member.getUserid(), member.getStudentUserId());

        Long departmentId = explicitDepartmentId != null ? explicitDepartmentId : member.getDepartmentId();
        if (departmentId != null) {
            context.studentDepartmentIds.put(member.getUserid(), departmentId);
        }
    }

    /** 綁定去重：同一 parent + student + department 只保留一筆，返回是否為新綁定 */
    private boolean tryAddBinding(ReceiverResolutionContext context, String parentUserId,
                                  String studentUserId, Long departmentId) {
        String uniqueKey = buildBindingKey(parentUserId, studentUserId, departmentId);
        if (context.bindingKeys.contains(uniqueKey)) {
            log.debug("跳過重複的綁定關係: parentUserId={}, studentUserId={}, departmentId={}",
                    parentUserId, studentUserId, departmentId);
            return false;
        }
        context.bindingKeys.add(uniqueKey);
        return true;
    }

    // -------------------------------------------------------------------------
    // 企微部門樹：遞歸找班級（type=1）
    // -------------------------------------------------------------------------

    /**
     * 將上層部門 ID 展開為所有 type=1 的班級部門 ID。
     * 若傳入的已是班級則直接返回。
     */
    private List<Long> resolveClassDepartmentIds(List<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysDepartment> allDepartments = sysDepartmentMapper.selectAll();
        if (allDepartments == null || allDepartments.isEmpty()) {
            return departmentIds;
        }

        Map<Long, SysDepartment> deptMap = allDepartments.stream()
                .filter(Objects::nonNull)
                .filter(dept -> dept.getId() != null)
                .collect(Collectors.toMap(SysDepartment::getId, dept -> dept, (a, b) -> a));

        Set<Long> classDepartmentIds = new HashSet<>();
        for (Long deptId : departmentIds) {
            SysDepartment dept = deptMap.get(deptId);
            if (dept == null) {
                continue;
            }
            if (Integer.valueOf(1).equals(dept.getType())) {
                classDepartmentIds.add(deptId);
            } else {
                collectClassDepartmentIds(deptId, allDepartments, classDepartmentIds);
            }
        }
        return new ArrayList<>(classDepartmentIds);
    }

    /** 遞歸收集父部門下所有 type=1 班級節點 */
    private void collectClassDepartmentIds(Long parentId, List<SysDepartment> allDepartments,
                                           Set<Long> classDepartmentIds) {
        if (parentId == null || allDepartments == null) {
            return;
        }

        List<SysDepartment> children = allDepartments.stream()
                .filter(Objects::nonNull)
                .filter(dept -> dept.getParentId() != null)
                .filter(dept -> dept.getParentId().longValue() == parentId)
                .collect(Collectors.toList());

        for (SysDepartment child : children) {
            if (child.getId() == null) {
                continue;
            }
            if (Integer.valueOf(1).equals(child.getType())) {
                classDepartmentIds.add(child.getId());
            } else {
                collectClassDepartmentIds(child.getId(), allDepartments, classDepartmentIds);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 後處理：補全映射、去重
    // -------------------------------------------------------------------------

    /** 記錄家長 userid → 學生 userid（同一家長只保留首次映射） */
    private void registerParentStudentMapping(ReceiverResolutionContext context,
                                              String parentUserId, String studentUserId) {
        if (!StringUtils.hasText(parentUserId) || !StringUtils.hasText(studentUserId)) {
            return;
        }
        context.parentStudentUserIds.putIfAbsent(parentUserId.trim(), studentUserId.trim());
    }

    /**
     * 校驗自定義家校成員是否已關聯學生 userid。
     * <p>
     * 企微選人走 bindings，不要求 parentStudentUserIds；
     * 自定義家校無 binding，必須依賴成員表 {@code sys_school_department_member.student_user_id}，
     * 不可用 {@code sys_parent_student_relation} 兜底（一家長多學生時無法判斷是哪個孩子）。
     * </p>
     */
    private void validateCustomMemberStudentUserId(ReceiverResolutionContext context,
                                                   boolean strictDepartmentCheck) {
        Set<String> coveredByBindings = context.bindings.stream()
                .map(SysDepartmentParentBinding::getParentUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (String parentUserId : context.parentUserIds) {
            if (coveredByBindings.contains(parentUserId)) {
                continue;
            }
            if (context.parentStudentUserIds.containsKey(parentUserId)) {
                continue;
            }
            String message = String.format(
                    "自定義家校成員缺少 student_user_id，家長 userid=%s，請在通訊錄中補全後再發送",
                    parentUserId);
            if (strictDepartmentCheck) {
                throw new ServiceException(message);
            }
            log.warn(message);
        }
    }

    /** 最終綁定列表去重（parent + student + department 為唯一鍵） */
    private List<SysDepartmentParentBinding> deduplicateBindings(List<SysDepartmentParentBinding> bindings) {
        List<SysDepartmentParentBinding> uniqueBindings = new ArrayList<>();
        Set<String> finalBindingKeys = new HashSet<>();
        for (SysDepartmentParentBinding binding : bindings) {
            if (binding.getParentUserId() == null || binding.getStudentUserId() == null) {
                continue;
            }
            String key = buildBindingKey(binding.getParentUserId(), binding.getStudentUserId(),
                    binding.getDepartmentId());
            if (!finalBindingKeys.contains(key)) {
                uniqueBindings.add(binding);
                finalBindingKeys.add(key);
            } else {
                log.warn("檢測到重複的綁定關係，已移除: parentUserId={}, studentUserId={}, departmentId={}",
                        binding.getParentUserId(), binding.getStudentUserId(), binding.getDepartmentId());
            }
        }
        return uniqueBindings;
    }

    /** 綁定唯一鍵：parentUserId_studentUserId_departmentId */
    private String buildBindingKey(String parentUserId, String studentUserId, Long departmentId) {
        return parentUserId + "_" + studentUserId + "_" + (departmentId != null ? departmentId : "null");
    }
}
