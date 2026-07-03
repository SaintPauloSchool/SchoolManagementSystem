package com.sms.system.service.impl.notification;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.common.exception.ServiceException;
import com.sms.common.utils.bean.BeanCopyUtils;
import com.sms.system.entity.SysSchoolDepartmentMember;
import com.sms.system.entity.SysSchoolFamilyContact;
import com.sms.system.entity.dto.NotificationReceiverSaveDTO;
import com.sms.system.entity.notification.NotificationReceiver;
import com.sms.system.entity.notification.receiver.ReceiverResolutionContext;
import com.sms.system.entity.vo.NotificationReceiverVO;
import com.sms.system.entity.vo.ResolvedReceiversVO;
import com.sms.system.enums.NotificationReceiverType;
import com.sms.system.mapper.SysSchoolDepartmentMemberMapper;
import com.sms.system.mapper.SysSchoolFamilyContactMapper;
import com.sms.system.mapper.notification.NotificationReceiverMapper;
import com.sms.system.service.notification.INotificationReceiverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知接收對象 Service。
 */
@Service
public class NotificationReceiverServiceImpl implements INotificationReceiverService {

    private static final Logger log = LoggerFactory.getLogger(NotificationReceiverServiceImpl.class);

    @Autowired
    private NotificationReceiverMapper notificationReceiverMapper;

    @Autowired
    private SysSchoolFamilyContactMapper schoolFamilyContactMapper;

    @Autowired
    private SysSchoolDepartmentMemberMapper schoolDepartmentMemberMapper;

    /**
     * 根據通知 ID 查詢接收對象列表。
     *
     * @param notificationId 通知 ID
     * @return 接收對象列表；無數據時返回空列表
     */
    @Override
    public List<NotificationReceiverVO> selectByNotificationId(Long notificationId) {
        List<NotificationReceiverVO> list = BeanCopyUtils.copyList(
                notificationReceiverMapper.selectByNotificationId(notificationId),
                NotificationReceiverVO.class);
        for (NotificationReceiverVO vo : list) {
            vo.setReceiveNames(resolveReceiveNames(vo.getReceiveType(), vo.getReceiveData()));
        }
        return list;
    }

    /**
     * 將 {@code receive_data} 中的 parentUserId 解析為展示姓名列表。
     *
     * @param receiveTypeCode 接收來源類型編碼，見 {@link NotificationReceiverType#getCode()}
     * @param receiveData     parentUserId 的 JSON 數組，如 {@code ["userid1","userid2"]}
     * @return 查詢到的姓名列表（不含查不到的成員）；數據無效或解析失敗時返回空列表
     */
    private List<String> resolveReceiveNames(String receiveTypeCode, String receiveData) {
        NotificationReceiverType receiveType = NotificationReceiverType.fromCode(receiveTypeCode);
        if (!StringUtils.hasText(receiveData) || receiveType == null) {
            return Collections.emptyList();
        }

        try {
            List<String> parentUserIds = parseParentUserIds(receiveData);
            if (parentUserIds.isEmpty()) {
                return Collections.emptyList();
            }
            return lookupReceiveNames(parentUserIds, receiveType);
        } catch (Exception e) {
            log.error("解析接收對象名稱失敗: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 按接收來源批量查詢家長展示姓名。
     *
     * @param parentUserIds parentUserId 列表（保持原順序）
     * @param receiveType   接收來源類型（WeCom / 自定義）
     * @return 查詢到的姓名列表（查不到的不返回）
     */
    private List<String> lookupReceiveNames(List<String> parentUserIds, NotificationReceiverType receiveType) {
        Map<String, String> nameMap = buildReceiveNameMap(parentUserIds, receiveType);
        return parentUserIds.stream()
                .map(nameMap::get)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    /**
     * 按接收來源批量查詢，構建 {@code parentUserId -> 展示姓名} 映射。
     *
     * @param parentUserIds 接收對象 parentUserId 列表（可含重複，內部會去重後查庫）
     * @param receiveType   接收來源類型（WeCom / 自定義）
     * @return parentUserId 與展示姓名的映射；類型不支持或無有效 ID 時返回空 Map
     */
    private Map<String, String> buildReceiveNameMap(List<String> parentUserIds, NotificationReceiverType receiveType) {
        List<String> uniqueIds = distinctNonBlankIds(parentUserIds);
        if (uniqueIds.isEmpty()) {
            return Collections.emptyMap();
        }
        if (receiveType == NotificationReceiverType.WECOM) {
            return buildWecomNameMap(uniqueIds);
        }
        if (receiveType == NotificationReceiverType.CUSTOM) {
            return buildCustomNameMap(uniqueIds);
        }
        return Collections.emptyMap();
    }

    /**
     * 批量查詢 WeCom 家校聯絡人，構建展示姓名映射。
     */
    private Map<String, String> buildWecomNameMap(List<String> parentUserIds) {
        List<SysSchoolFamilyContact> contacts = schoolFamilyContactMapper.selectByParentUserIds(parentUserIds);
        if (contacts == null || contacts.isEmpty()) {
            return Collections.emptyMap();
        }
        return contacts.stream()
                .filter(contact -> StringUtils.hasText(contact.getParentUserId()))
                .collect(Collectors.toMap(
                        SysSchoolFamilyContact::getParentUserId,
                        this::buildWecomDisplayName,
                        (left, right) -> left
                ));
    }

    /**
     * 批量查詢自定義家校成員，構建展示姓名映射。
     */
    private Map<String, String> buildCustomNameMap(List<String> userids) {
        return collectStringNameMap(
                schoolDepartmentMemberMapper.selectMembersByUserids(userids),
                SysSchoolDepartmentMember::getUserid,
                SysSchoolDepartmentMember::getName
        );
    }

    /**
     * 將列表收集為 {@code key -> name} 映射，僅保留 key 與 name 均有效的項。
     */
    private <T> Map<String, String> collectStringNameMap(List<T> items,
                                                         Function<T, String> keyGetter,
                                                         Function<T, String> nameGetter) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyMap();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .filter(item -> StringUtils.hasText(keyGetter.apply(item))
                        && StringUtils.hasText(nameGetter.apply(item)))
                .collect(Collectors.toMap(keyGetter, nameGetter, (left, right) -> left));
    }

    /**
     * 構建 WeCom 家校聯絡人的展示姓名。
     * <p>格式：{@code 學生姓名-關係描述}；無關係描述時僅返回學生姓名。</p>
     *
     * @param contact 家校聯絡人記錄
     * @return 展示姓名；學生姓名缺失時使用「未知」
     */
    private String buildWecomDisplayName(SysSchoolFamilyContact contact) {
        String studentName = StringUtils.hasText(contact.getStudentName()) ? contact.getStudentName() : "未知";
        String relationDesc = contact.getRelationDesc();
        if (StringUtils.hasText(relationDesc)) {
            return studentName + "-" + relationDesc;
        }
        return studentName;
    }

    /**
     * 新增通知接收對象記錄。
     * <p>{@code receive_type} 為來源類型，{@code receive_data} 為 parentUserId JSON 數組。</p>
     *
     * @param notificationReceiverSaveDTO 接收對象保存參數
     * @return 插入行數
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int save(NotificationReceiverSaveDTO notificationReceiverSaveDTO) {
        NotificationReceiver receiver = BeanCopyUtils.copy(notificationReceiverSaveDTO, NotificationReceiver.class);
        if (receiver.getCreateTime() == null) {
            receiver.setCreateTime(LocalDateTime.now());
        }
        return notificationReceiverMapper.insert(receiver);
    }

    /**
     * 解析通知接收對象，得到實際發送所需的家長、學生及關係信息。
     *
     * @param receivers             通知接收記錄列表
     * @param strictDepartmentCheck 是否嚴格校驗部門綁定（發佈時為 {@code true}，定時任務可為 {@code false}）
     * @return 解析後的接收人信息；無接收對象時返回空結果
     */
    @Override
    public ResolvedReceiversVO resolveReceivers(List<NotificationReceiver> receivers, boolean strictDepartmentCheck) {
        ReceiverResolutionContext context = new ReceiverResolutionContext();

        if (receivers == null || receivers.isEmpty()) {
            log.warn("通知接收者为空");
            return context.toResult();
        }

        try {
            for (NotificationReceiver receiver : receivers) {
                parseReceiver(receiver, context, strictDepartmentCheck);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("无法解析通知接收者", e);
            throw new ServiceException("解析接收對象失敗：" + e.getMessage());
        }

        validateCustomMemberStudentUserId(context, strictDepartmentCheck);
        List<SysSchoolFamilyContact> uniqueRelations = deduplicateRelations(context.relations);

        log.info("解析完成 - parentUserIds: {}, studentUserIds: {}, 關係數: {} (去重前: {})",
                context.parentUserIds.size(), context.studentUserIds.size(),
                uniqueRelations.size(), context.relations.size());

        return context.toResult(uniqueRelations);
    }

    /**
     * 解析單條接收記錄，按來源類型分發到對應解析邏輯。
     *
     * @param receiver              接收記錄（含 {@code receive_type}、{@code receive_data}）
     * @param context               解析上下文，用於累積結果
     * @param strictDepartmentCheck 是否嚴格校驗部門綁定（僅 WeCom 路徑使用）
     */
    private void parseReceiver(NotificationReceiver receiver, ReceiverResolutionContext context,
                               boolean strictDepartmentCheck) {
        // 校驗來源類型與 receive_data
        NotificationReceiverType receiveType = NotificationReceiverType.fromCode(receiver.getReceiveType());
        if (receiveType == null || !StringUtils.hasText(receiver.getReceiveData())) {
            return;
        }

        // 解析 receive_data：["parentUserId1", "parentUserId2", ...]
        List<String> parentUserIds = parseParentUserIds(receiver.getReceiveData());
        if (parentUserIds.isEmpty()) {
            return;
        }

        // 按來源類型解析為實際發送目標
        if (receiveType == NotificationReceiverType.WECOM) {
            resolveWecomReceivers(parentUserIds, context, strictDepartmentCheck);
        } else if (receiveType == NotificationReceiverType.CUSTOM) {
            resolveCustomReceivers(parentUserIds, context);
        }
    }

    /**
     * 解析 {@code receive_data} 為 parentUserId 列表。
     *
     * @param receiveData JSON 數組字符串，如 {@code ["userid1","userid2"]}
     * @return parentUserId 列表（保留原順序，跳過空白項）；空或無效時返回空列表
     */
    private List<String> parseParentUserIds(String receiveData) {
        JSONArray array = JSONObject.parseArray(receiveData);
        if (array == null || array.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> parentUserIds = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            String parentUserId = array.getString(i);
            if (StringUtils.hasText(parentUserId)) {
                parentUserIds.add(parentUserId.trim());
            }
        }
        return parentUserIds;
    }

    /**
     * 解析 WeCom 家校通訊錄接收人。
     * <p>同一 parentUserId 被選中多次時，按順序匹配尚未使用的家長-學生關係。</p>
     *
     * @param parentUserIds         接收對象 parentUserId 列表（保留原順序）
     * @param context               解析上下文
     * @param strictDepartmentCheck 是否嚴格校驗部門綁定
     */
    private void resolveWecomReceivers(List<String> parentUserIds, ReceiverResolutionContext context,
                                       boolean strictDepartmentCheck) {
        // 去重後批量查詢聯絡人
        List<String> uniqueIds = distinctNonBlankIds(parentUserIds);
        if (uniqueIds.isEmpty()) {
            return;
        }

        List<SysSchoolFamilyContact> contacts = schoolFamilyContactMapper.selectByParentUserIds(uniqueIds);
        if (contacts == null || contacts.isEmpty()) {
            return;
        }

        // 按 parentUserId 分組，便於同一家長多條關係時逐條匹配
        Map<String, List<SysSchoolFamilyContact>> contactsByParent = contacts.stream()
                .filter(contact -> StringUtils.hasText(contact.getParentUserId()))
                .collect(Collectors.groupingBy(SysSchoolFamilyContact::getParentUserId));

        // 預加載學生維度的全部關係，用於解析部門綁定
        Map<String, List<SysSchoolFamilyContact>> schoolFamilyContactsMap = loadSchoolFamilyContactsMap(contacts);

        // 按原選擇順序逐個處理 parentUserId
        for (String parentUserId : parentUserIds) {
            if (!StringUtils.hasText(parentUserId)) {
                continue;
            }
            List<SysSchoolFamilyContact> candidates = contactsByParent.get(parentUserId);
            if (candidates == null || candidates.isEmpty()) {
                continue;
            }

            // 選取下一條尚未使用的家長-學生關係
            SysSchoolFamilyContact relation = pickNextRelation(candidates, context.relationKeys);
            if (relation == null) {
                continue;
            }
            registerWecomRelation(context, parentUserId, relation, schoolFamilyContactsMap, strictDepartmentCheck);
        }
    }

    /**
     * 將 WeCom 家長-學生關係寫入解析上下文。
     */
    private void registerWecomRelation(ReceiverResolutionContext context, String parentUserId,
                                       SysSchoolFamilyContact relation,
                                       Map<String, List<SysSchoolFamilyContact>> schoolFamilyContactsMap,
                                       boolean strictDepartmentCheck) {
        context.parentUserIds.add(parentUserId);
        String studentUserId = relation.getStudentUserId();
        SysSchoolFamilyContact resolvedRelation = resolveRelationDepartment(
                relation,
                schoolFamilyContactsMap.get(parentStudentKey(parentUserId, studentUserId)),
                strictDepartmentCheck);
        Long departmentId = resolvedRelation != null ? resolvedRelation.getDepartmentId() : relation.getDepartmentId();

        if (!tryAddRelation(context, parentUserId, studentUserId, departmentId)) {
            return;
        }

        if (resolvedRelation != null) {
            context.relations.add(resolvedRelation);
            return;
        }

        SysSchoolFamilyContact fallback = new SysSchoolFamilyContact();
        fallback.setParentUserId(parentUserId);
        fallback.setStudentUserId(studentUserId);
        fallback.setStudentName(relation.getStudentName());
        fallback.setDepartmentId(departmentId);
        context.relations.add(fallback);
    }

    /**
     * 從候選關係中選取下一條尚未使用的記錄。
     */
    private SysSchoolFamilyContact pickNextRelation(List<SysSchoolFamilyContact> candidates, Set<String> usedKeys) {
        for (SysSchoolFamilyContact candidate : candidates) {
            if (candidate == null || !StringUtils.hasText(candidate.getParentUserId())) {
                continue;
            }
            String key = buildRelationKey(candidate.getParentUserId(), candidate.getStudentUserId(),
                    candidate.getDepartmentId());
            if (!usedKeys.contains(key)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 解析自定義家校通訊錄接收人。
     */
    private void resolveCustomReceivers(List<String> parentUserIds, ReceiverResolutionContext context) {
        List<String> uniqueIds = distinctNonBlankIds(parentUserIds);
        if (uniqueIds.isEmpty()) {
            return;
        }

        List<SysSchoolDepartmentMember> members = schoolDepartmentMemberMapper.selectMembersByUserids(uniqueIds);
        if (members == null || members.isEmpty()) {
            return;
        }

        Map<String, SysSchoolDepartmentMember> memberMap = members.stream()
                .filter(member -> StringUtils.hasText(member.getUserid()))
                .collect(Collectors.toMap(SysSchoolDepartmentMember::getUserid, member -> member, (a, b) -> a));

        for (String parentUserId : parentUserIds) {
            if (!StringUtils.hasText(parentUserId)) {
                continue;
            }
            SysSchoolDepartmentMember member = memberMap.get(parentUserId);
            if (member != null) {
                registerCustomMember(context, member);
            }
        }
    }

    /** 去重並過濾空白 parentUserId。 */
    private List<String> distinctNonBlankIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 按學生 userid 批量加載家長-學生關係，用於解析部門綁定。
     */
    private Map<String, List<SysSchoolFamilyContact>> loadSchoolFamilyContactsMap(
            List<SysSchoolFamilyContact> relations) {
        List<String> studentUserIds = relations.stream()
                .map(SysSchoolFamilyContact::getStudentUserId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (studentUserIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<SysSchoolFamilyContact> existingRelations =
                schoolFamilyContactMapper.selectByStudentUserIds(studentUserIds);
        if (existingRelations == null || existingRelations.isEmpty()) {
            return Collections.emptyMap();
        }

        return existingRelations.stream()
                .filter(relation -> relation.getStudentUserId() != null && relation.getParentUserId() != null)
                .collect(Collectors.groupingBy(
                        relation -> parentStudentKey(relation.getParentUserId(), relation.getStudentUserId())
                ));
    }

    /**
     * 解析家長-學生關係對應的部門綁定。
     * <p>優先使用聯絡人記錄自帶的 {@code departmentId}；否則從候選關係中推斷。</p>
     */
    private SysSchoolFamilyContact resolveRelationDepartment(SysSchoolFamilyContact relation,
                                                             List<SysSchoolFamilyContact> candidateRelations,
                                                             boolean strictDepartmentCheck) {
        if (relation.getDepartmentId() != null) {
            return relation;
        }
        if (candidateRelations == null || candidateRelations.isEmpty()) {
            return null;
        }
        if (candidateRelations.size() == 1) {
            return candidateRelations.get(0);
        }
        if (!strictDepartmentCheck) {
            log.warn("家長學生關係存在多個部門綁定，已使用第一筆: parentUserId={}, studentUserId={}",
                    relation.getParentUserId(), relation.getStudentUserId());
            return candidateRelations.get(0);
        }
        throw new ServiceException(String.format(
                "家長「%s」存在多個班級綁定，無法自動確定發送班級（parentUserId=%s）",
                relation.getStudentName() != null ? relation.getStudentName() : relation.getParentUserId(),
                relation.getParentUserId()));
    }

    private void registerCustomMember(ReceiverResolutionContext context, SysSchoolDepartmentMember member) {
        context.parentUserIds.add(member.getUserid());
        registerParentStudentMapping(context, member.getUserid(), member.getStudentUserId());
        if (member.getDepartmentId() != null) {
            context.studentDepartmentIds.put(member.getUserid(), member.getDepartmentId());
        }
    }

    private boolean tryAddRelation(ReceiverResolutionContext context, String parentUserId,
                                   String studentUserId, Long departmentId) {
        String uniqueKey = buildRelationKey(parentUserId, studentUserId, departmentId);
        if (context.relationKeys.contains(uniqueKey)) {
            log.debug("跳過重複的關係: parentUserId={}, studentUserId={}, departmentId={}",
                    parentUserId, studentUserId, departmentId);
            return false;
        }
        context.relationKeys.add(uniqueKey);
        return true;
    }

    private void registerParentStudentMapping(ReceiverResolutionContext context,
                                              String parentUserId, String studentUserId) {
        if (!StringUtils.hasText(parentUserId) || !StringUtils.hasText(studentUserId)) {
            return;
        }
        context.parentStudentUserIds.putIfAbsent(parentUserId.trim(), studentUserId.trim());
    }

    private void validateCustomMemberStudentUserId(ReceiverResolutionContext context,
                                                   boolean strictDepartmentCheck) {
        Set<String> coveredByRelations = context.relations.stream()
                .map(SysSchoolFamilyContact::getParentUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (String parentUserId : context.parentUserIds) {
            if (coveredByRelations.contains(parentUserId)) {
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

    private List<SysSchoolFamilyContact> deduplicateRelations(List<SysSchoolFamilyContact> relations) {
        List<SysSchoolFamilyContact> uniqueRelations = new ArrayList<>();
        Set<String> finalRelationKeys = new HashSet<>();
        for (SysSchoolFamilyContact relation : relations) {
            if (relation.getParentUserId() == null || relation.getStudentUserId() == null) {
                continue;
            }
            String key = buildRelationKey(relation.getParentUserId(), relation.getStudentUserId(),
                    relation.getDepartmentId());
            if (!finalRelationKeys.contains(key)) {
                uniqueRelations.add(relation);
                finalRelationKeys.add(key);
            } else {
                log.warn("檢測到重複的關係，已移除: parentUserId={}, studentUserId={}, departmentId={}",
                        relation.getParentUserId(), relation.getStudentUserId(), relation.getDepartmentId());
            }
        }
        return uniqueRelations;
    }

    /**
     * 構建家長-學生組合鍵，用於按學生維度分組聯絡人。
     *
     * @param parentUserId  家長企微 userid
     * @param studentUserId 學生企微 userid
     * @return 格式為 {@code parentUserId_studentUserId}
     */
    private String parentStudentKey(String parentUserId, String studentUserId) {
        return parentUserId + "_" + studentUserId;
    }

    /**
     * 構建家長-學生-部門唯一鍵，用於去重與判斷關係是否已處理。
     *
     * @param parentUserId  家長企微 userid
     * @param studentUserId 學生企微 userid
     * @param departmentId  部門 ID；為 {@code null} 時以字面量 {@code "null"} 佔位
     * @return 格式為 {@code parentUserId_studentUserId_departmentId}
     */
    private String buildRelationKey(String parentUserId, String studentUserId, Long departmentId) {
        return parentUserId + "_" + studentUserId + "_" + (departmentId != null ? departmentId : "null");
    }
}
