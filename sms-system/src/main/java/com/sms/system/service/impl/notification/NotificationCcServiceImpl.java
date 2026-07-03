package com.sms.system.service.impl.notification;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.common.utils.bean.BeanCopyUtils;
import com.sms.system.entity.SysSchoolDepartmentMember;
import com.sms.system.entity.WecomSchoolDepartmentMember;
import com.sms.system.entity.dto.NotificationCcSaveDTO;
import com.sms.system.entity.notification.NotificationCc;
import com.sms.system.entity.vo.NotificationCcVO;
import com.sms.system.enums.NotificationCcType;
import com.sms.system.mapper.SysSchoolDepartmentMemberMapper;
import com.sms.system.mapper.WecomSchoolDepartmentMemberMapper;
import com.sms.system.mapper.notification.NotificationCcMapper;
import com.sms.system.service.notification.INotificationCcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 通知抄送對象 Service 業務層處理
 */
@Service
public class NotificationCcServiceImpl implements INotificationCcService {

    private static final Logger log = LoggerFactory.getLogger(NotificationCcServiceImpl.class);

    @Autowired
    private NotificationCcMapper notificationCcMapper;

    @Autowired
    private WecomSchoolDepartmentMemberMapper wecomSchoolDepartmentMemberMapper;

    @Autowired
    private SysSchoolDepartmentMemberMapper sysSchoolDepartmentMemberMapper;

    /**
     * 根據通知 ID 查詢抄送對象列表。
     *
     * @param notificationId 通知 ID
     * @return 抄送對象列表；無數據時返回空列表
     */
    @Override
    public List<NotificationCcVO> selectByNotificationId(Long notificationId) {
        List<NotificationCcVO> list = BeanCopyUtils.copyList(
                notificationCcMapper.selectByNotificationId(notificationId), NotificationCcVO.class);
        for (NotificationCcVO vo : list) {
            vo.setCcNames(resolveCcNames(vo.getCcType(), vo.getCcData()));
        }
        return list;
    }

    /**
     * 將 {@code cc_data} 中的成員 ID 解析為姓名列表。
     *
     * @param ccTypeCode 抄送來源類型編碼，見 {@link NotificationCcType#getCode()}
     * @param ccData     抄送成員 ID 的 JSON 數組，如 {@code [1,2,3]}
     * @return 查詢到的姓名列表（不含查不到的成員）；數據無效或解析失敗時返回空列表
     */
    private List<String> resolveCcNames(String ccTypeCode, String ccData) {
        // 校驗抄送來源類型（1=WeCom，2=自定義）
        NotificationCcType ccType = NotificationCcType.fromCode(ccTypeCode);
        if (!StringUtils.hasText(ccData) || ccType == null) {
            return Collections.emptyList();
        }

        try {
            // 解析 cc_data：[1,2,3]
            List<Long> ids = parseMemberIds(ccData);
            if (ids.isEmpty()) {
                return Collections.emptyList();
            }
            // 按來源類型查成員姓名，順序與 ids 一致
            return lookupMemberNames(ids, ccType);
        } catch (Exception e) {
            log.error("解析抄送名稱失敗: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 按抄送來源批量查詢成員姓名。
     *
     * @param ids    成員 ID 列表
     * @param ccType 抄送來源類型（WeCom / 自定義）
     * @return 查詢到的姓名列表（查不到的不返回）
     */
    private List<String> lookupMemberNames(List<Long> ids, NotificationCcType ccType) {
        // 按來源類型批量查詢 id -> name 映射
        Map<Long, String> nameMap = buildMemberNameMap(ids, ccType);
        // 保持 ids 順序，僅返回能查到姓名的項
        return ids.stream()
                .map(nameMap::get)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    /**
     * 按抄送來源批量查詢成員，構建 {@code 成員ID -> 姓名} 映射。
     *
     * @param ids    成員 ID 列表
     * @param ccType 抄送來源類型（WeCom / 自定義）
     * @return 成員 ID 與姓名的映射；無數據時返回空 Map
     */
    private Map<Long, String> buildMemberNameMap(List<Long> ids, NotificationCcType ccType) {
        if (ccType == NotificationCcType.WECOM) {
            return collectNameMap(
                    wecomSchoolDepartmentMemberMapper.selectMembersByIds(ids),
                    WecomSchoolDepartmentMember::getId,
                    WecomSchoolDepartmentMember::getName
            );
        }
        if (ccType == NotificationCcType.CUSTOM) {
            return collectNameMap(
                    sysSchoolDepartmentMemberMapper.selectMembersByIds(ids),
                    SysSchoolDepartmentMember::getId,
                    SysSchoolDepartmentMember::getName
            );
        }
        return Collections.emptyMap();
    }

    /**
     * 將成員列表收集為 {@code id -> name} 映射，僅保留有有效姓名的成員。
     */
    private <T> Map<Long, String> collectNameMap(List<T> members, Function<T, Long> idGetter, Function<T, String> nameGetter) {
        if (members == null || members.isEmpty()) {
            return Collections.emptyMap();
        }
        return members.stream()
                .filter(Objects::nonNull)
                .filter(member -> StringUtils.hasText(nameGetter.apply(member)))
                .collect(Collectors.toMap(
                        idGetter,
                        nameGetter,
                        (left, right) -> left
                ));
    }

    /**
     * 新增通知抄送記錄。
     *
     * @param notificationCcSaveDTO 抄送保存參數
     * @return 插入行數
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int save(NotificationCcSaveDTO notificationCcSaveDTO) {
        NotificationCc cc = BeanCopyUtils.copy(notificationCcSaveDTO, NotificationCc.class);
        // 未傳創建時間時使用當前時間
        if (cc.getCreateTime() == null) {
            cc.setCreateTime(LocalDateTime.now());
        }
        return notificationCcMapper.insert(cc);
    }


    /**
     * 解析通知的全部抄送配置，合併得到企業微信 userid 集合。
     *
     * @param ccs 通知抄送記錄列表
     * @return 去重後的 userid 集合；無有效抄送時返回空集合
     */
    @Override
    public Set<String> resolveCcUserIds(List<NotificationCc> ccs) {
        if (ccs == null || ccs.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> userIds = new HashSet<>();
        // 逐條解析並合併 userid
        for (NotificationCc cc : ccs) {
            userIds.addAll(resolveCcUserIds(cc));
        }
        return userIds;
    }

    /**
     * 解析單條抄送記錄，獲取對應的 userid 集合。
     *
     * @param cc 抄送記錄
     * @return userid 集合；數據無效或類型不支持時返回空集合
     */
    private Set<String> resolveCcUserIds(NotificationCc cc) {
        if (cc == null || !StringUtils.hasText(cc.getCcData())) {
            return Collections.emptySet();
        }

        NotificationCcType ccType = NotificationCcType.fromCode(cc.getCcType());
        if (ccType == null) {
            log.warn("忽略不支持的抄送類型 ccType={}", cc.getCcType());
            return Collections.emptySet();
        }

        try {
            // 解析 cc_data：[1,2,3]，再按來源類型查 userid
            List<Long> ids = parseMemberIds(cc.getCcData());
            return ids.isEmpty() ? Collections.emptySet() : lookupUserIds(ids, ccType);
        } catch (Exception e) {
            log.error("解析抄送數據失敗: {}", e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    /**
     * 按抄送來源批量查詢成員，提取企業微信 userid。
     *
     * @param ids    成員 ID 列表
     * @param ccType 抄送來源類型（WeCom / 自定義）
     * @return 去重後的 userid 集合；類型不支持或無有效成員時返回空集合
     */
    private Set<String> lookupUserIds(List<Long> ids, NotificationCcType ccType) {
        if (ccType == NotificationCcType.WECOM) {
            // WeCom 老師通訊錄
            List<WecomSchoolDepartmentMember> members = wecomSchoolDepartmentMemberMapper.selectMembersByIds(ids);
            return collectUserIds(members, WecomSchoolDepartmentMember::getUserid, member -> true);
        }
        if (ccType == NotificationCcType.CUSTOM) {
            // 自定義老師通訊錄（僅教職員工 type=1）
            List<SysSchoolDepartmentMember> members = sysSchoolDepartmentMemberMapper.selectMembersByIds(ids);
            return collectUserIds(members, SysSchoolDepartmentMember::getUserid,
                    member -> member.getType() != null && member.getType() == 1);
        }
        return Collections.emptySet();
    }

    /**
     * 從成員列表中提取 userid 集合。
     *
     * @param members      成員列表
     * @param userIdGetter 提取 userid
     * @param filter       成員過濾條件
     * @return 去重後的 userid 集合
     */
    private <T> Set<String> collectUserIds(List<T> members, Function<T, String> userIdGetter, Predicate<T> filter) {
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }
        return members.stream()
                .filter(Objects::nonNull)
                .filter(filter)
                .map(userIdGetter)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    /**
     * 查詢抄送給指定成員的所有通知 ID（「抄送給我」列表用）。
     *
     * @param userId 當前用戶對應的通訊錄成員 ID（與 {@code cc_data} 中存儲的 ID 一致）
     * @return 通知 ID 集合；無匹配時返回空集合
     */
    @Override
    public Set<Long> selectNotificationIdsByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }

        List<NotificationCc> allCcList = notificationCcMapper.selectAll();
        if (allCcList == null || allCcList.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> notificationIds = new HashSet<>();
        for (NotificationCc cc : allCcList) {
            // 僅處理支持的來源類型，且 cc_data 包含當前成員 ID
            if (NotificationCcType.isSupported(cc.getCcType()) && containsMemberId(cc.getCcData(), userId)) {
                notificationIds.add(cc.getNotificationId());
            }
        }
        return notificationIds;
    }

    /**
     * 判斷 {@code cc_data} 中是否包含指定成員 ID。
     *
     * @param ccData   抄送成員 ID 的 JSON 數組
     * @param memberId 成員 ID
     * @return 包含返回 {@code true}，否則返回 {@code false}
     */
    private boolean containsMemberId(String ccData, Long memberId) {
        if (!StringUtils.hasText(ccData) || memberId == null) {
            return false;
        }
        try {
            return parseMemberIds(ccData).contains(memberId);
        } catch (Exception e) {
            log.error("解析抄送數據失敗: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 解析 {@code cc_data} 為成員 ID 列表。
     *
     * @param ccData JSON 數組字符串，如 {@code [1,2,3]}
     * @return 成員 ID 列表；空或解析失敗時返回空列表
     */
    private List<Long> parseMemberIds(String ccData) {
        JSONArray array = JSONObject.parseArray(ccData);
        if (array == null || array.isEmpty()) {
            return Collections.emptyList();
        }
        return array.toJavaList(Long.class);
    }

}
