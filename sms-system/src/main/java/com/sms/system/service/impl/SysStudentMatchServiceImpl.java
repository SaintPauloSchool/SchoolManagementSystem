package com.sms.system.service.impl;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.sms.common.config.StudentProfilesProperties;
import com.sms.common.core.page.PageDomain;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.core.page.TableSupport;
import com.sms.system.entity.SysSchoolFamilyContact;
import com.sms.system.entity.SysStudentMatch;
import com.sms.system.entity.dto.*;
import com.sms.system.entity.vo.*;
import com.sms.system.mapper.SysSchoolFamilyContactMapper;
import com.sms.system.mapper.SysStudentMatchMapper;
import com.sms.system.service.ISysStudentMatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 學生數據匹配 Service 實現類
 * <p>學籍資料來自 student_profiles.student_info；匹配結果寫入 sys_student_match（student_id + 家長 parent_user_id）。</p>
 */
@Service
public class SysStudentMatchServiceImpl implements ISysStudentMatchService {

    @Autowired
    private SysStudentMatchMapper sysStudentMatchMapper;

    @Autowired
    private SysSchoolFamilyContactMapper schoolFamilyContactMapper;

    @Autowired
    private StudentProfilesProperties studentProfilesProperties;

    private String studentProfilesDatabase() {
        return studentProfilesProperties.getDatabase();
    }

    private SysStudentMatchVO getMatchVOById(Long matchId) {
        return sysStudentMatchMapper.selectStudentMatchVOById(matchId, studentProfilesDatabase());
    }

    private Long findMatchIdByStudentId(String studentId) {
        if (!StringUtils.hasText(studentId)) {
            return null;
        }
        return sysStudentMatchMapper.selectMatchIdByStudentId(studentId.trim());
    }

    private boolean saveMatchedRecord(SysStudentMatch studentMatch) {
        if (studentMatch == null || !StringUtils.hasText(studentMatch.getStudentId())) {
            return false;
        }
        studentMatch.setStudentId(studentMatch.getStudentId().trim());
        if (studentMatch.getId() != null) {
            return sysStudentMatchMapper.updateStudentMatch(studentMatch) > 0;
        }
        return sysStudentMatchMapper.saveOrUpdateStudentMatch(studentMatch) > 0;
    }

    private void prepareWecomNameQuery(SysWecomStudentDTO wecomStudentDTO) {
        if (wecomStudentDTO == null || !StringUtils.hasText(wecomStudentDTO.getQueryName())) {
            return;
        }
        String queryName = wecomStudentDTO.getQueryName().trim();
        wecomStudentDTO.setQueryName(queryName);
        wecomStudentDTO.setQueryNameTraditional(ZhConverterUtil.toTraditional(queryName));
        wecomStudentDTO.setQueryNameSimplified(ZhConverterUtil.toSimple(queryName));
    }

    @Override
    public List<SysStudentMatchVO> selectSysStudentMatchList(SysStudentMatchDTO studentMatchDTO) {
        return sysStudentMatchMapper.selectSysStudentMatchList(studentMatchDTO, studentProfilesDatabase());
    }

    @Override
    public List<SysStudentMatchVO> selectUnmatchedList(SysStudentMatchDTO studentMatchDTO) {
        return sysStudentMatchMapper.selectUnmatchedList(studentMatchDTO, studentProfilesDatabase());
    }

    @Override
    public TableDataInfo selectWecomCandidates(SysWecomStudentDTO wecomStudentDTO) {
        SysWecomStudentDTO query = wecomStudentDTO != null ? wecomStudentDTO : new SysWecomStudentDTO();
        prepareWecomNameQuery(query);

        Set<String> matchedClassNameKeys = loadMatchedClassNameKeys();
        List<SysSchoolFamilyContactVO> filtered = schoolFamilyContactMapper.selectSchoolFamilyContactWithClassList()
                .stream()
                .filter(contact -> StringUtils.hasText(contact.getParentUserId()))
                .filter(contact -> matchesWecomCandidate(contact, query))
                .filter(contact -> !isContactClassNameAlreadyMatched(contact, matchedClassNameKeys))
                .sorted(Comparator.comparing(SysSchoolFamilyContactVO::getStudentName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());

        PageDomain pageDomain = TableSupport.buildPageRequest();
        int pageNum = pageDomain.getPageNum() != null ? pageDomain.getPageNum() : 1;
        int pageSize = pageDomain.getPageSize() != null ? pageDomain.getPageSize() : 10;
        int total = filtered.size();
        int fromIndex = Math.max((pageNum - 1) * pageSize, 0);
        List<SysSchoolFamilyContactVO> pageRows = fromIndex >= total
                ? Collections.emptyList()
                : filtered.subList(fromIndex, Math.min(fromIndex + pageSize, total));

        TableDataInfo data = new TableDataInfo();
        data.setCode(0);
        data.setRows(pageRows);
        data.setTotal(total);
        return data;
    }

    private Set<String> loadMatchedClassNameKeys() {
        List<SysStudentMatchVO> matchedList = sysStudentMatchMapper.selectSysStudentMatchList(
                new SysStudentMatchDTO(), studentProfilesDatabase());
        Set<String> keys = new HashSet<>();
        if (matchedList == null) {
            return keys;
        }
        for (SysStudentMatchVO matchVO : matchedList) {
            if (matchVO.getId() == null) {
                continue;
            }
            String classSection = matchVO.getClassSection() != null ? matchVO.getClassSection().trim() : "";
            String idName = matchVO.getIdName() != null ? matchVO.getIdName().trim() : "";
            if (classSection.isEmpty() || idName.isEmpty()) {
                continue;
            }
            keys.add(buildClassNameKey(classSection, idName));
        }
        return keys;
    }

    private String buildClassNameKey(String classSection, String name) {
        return classSection.toLowerCase(Locale.ROOT) + "|" + ZhConverterUtil.toTraditional(name.trim());
    }

    private boolean isContactClassNameAlreadyMatched(SysSchoolFamilyContactVO contact, Set<String> matchedClassNameKeys) {
        if (contact == null || matchedClassNameKeys.isEmpty()) {
            return false;
        }
        String classSection = contact.getClassCodeWecom() != null ? contact.getClassCodeWecom().trim() : "";
        String studentName = contact.getStudentName() != null ? contact.getStudentName().trim() : "";
        if (classSection.isEmpty() || studentName.isEmpty()) {
            return false;
        }
        return matchedClassNameKeys.contains(buildClassNameKey(classSection, studentName));
    }

    private boolean matchesWecomCandidate(SysSchoolFamilyContactVO student, SysWecomStudentDTO wecomStudentDTO) {
        if (!matchesWecomName(student, wecomStudentDTO)) {
            return false;
        }
        if (StringUtils.hasText(wecomStudentDTO.getQueryMobile())) {
            String mobile = student.getMobile();
            if (!StringUtils.hasText(mobile) || !mobile.contains(wecomStudentDTO.getQueryMobile().trim())) {
                return false;
            }
        }
        if (StringUtils.hasText(wecomStudentDTO.getQueryClass())) {
            String classCode = student.getClassCodeWecom();
            String queryClass = wecomStudentDTO.getQueryClass().trim();
            if (!StringUtils.hasText(classCode)
                    || !classCode.toLowerCase(Locale.ROOT).contains(queryClass.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesWecomName(SysSchoolFamilyContactVO student, SysWecomStudentDTO wecomStudentDTO) {
        if (!StringUtils.hasText(wecomStudentDTO.getQueryNameTraditional())) {
            return true;
        }
        String studentName = student.getStudentName();
        if (!StringUtils.hasText(studentName)) {
            return false;
        }
        String nameTraditional = ZhConverterUtil.toTraditional(studentName.trim());
        String nameSimplified = ZhConverterUtil.toSimple(studentName.trim());
        return nameTraditional.contains(wecomStudentDTO.getQueryNameTraditional())
                || nameSimplified.contains(wecomStudentDTO.getQueryNameSimplified());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysStudentMatchOperationResultVO bindStudent(SysStudentMatchBindDTO studentMatchBindDTO) {
        if (studentMatchBindDTO == null
                || !StringUtils.hasText(studentMatchBindDTO.getUserId())) {
            return SysStudentMatchOperationResultVO.failure(
                    "參數錯誤，請確認 matchId / studentId 和 userId（家長 parent_user_id）是否為空");
        }

        Long matchId = studentMatchBindDTO.getMatchId();
        String studentId = studentMatchBindDTO.getStudentId();
        if (matchId == null && !StringUtils.hasText(studentId)) {
            return SysStudentMatchOperationResultVO.failure(
                    "參數錯誤，請確認 matchId / studentId 和 userId 是否為空");
        }

        if (!StringUtils.hasText(studentId) && matchId != null) {
            SysStudentMatchVO matchVO = getMatchVOById(matchId);
            studentId = resolveStudentId(matchVO);
        }
        if (!StringUtils.hasText(studentId)) {
            return SysStudentMatchOperationResultVO.failure("綁定失敗，缺少學生 ID");
        }
        if (matchId == null) {
            matchId = findMatchIdByStudentId(studentId);
        }

        SysStudentMatch studentMatch = new SysStudentMatch();
        studentMatch.setId(matchId);
        studentMatch.setStudentId(studentId.trim());
        studentMatch.setUserId(studentMatchBindDTO.getUserId().trim());
        studentMatch.setMatchStatus("2");

        boolean saved = saveMatchedRecord(studentMatch);
        return saved
                ? SysStudentMatchOperationResultVO.success("綁定成功")
                : SysStudentMatchOperationResultVO.failure("綁定失敗");
    }

    @Override
    public List<SysStudentMatchVO> getPendingListForSync(SysStudentMatchSyncDTO studentMatchSyncDTO) {
        List<SysStudentMatchVO> result = new ArrayList<>();
        if (studentMatchSyncDTO == null || studentMatchSyncDTO.getMatchIds() == null) {
            return result;
        }
        for (Long matchId : studentMatchSyncDTO.getMatchIds()) {
            SysStudentMatchVO matchVO = getMatchVOById(matchId);
            if (matchVO != null
                    && StringUtils.hasText(matchVO.getUserId())
                    && StringUtils.hasText(resolveStudentUserIdForMatch(matchVO))) {
                result.add(matchVO);
            }
        }
        return result;
    }

    @Override
    public SysStudentMatchDeptMapVO getStudentDeptMap(SysStudentMatchDeptQueryDTO sysStudentMatchDeptQueryDTO) {
        SysStudentMatchDeptMapVO deptMapVO = new SysStudentMatchDeptMapVO();
        if (sysStudentMatchDeptQueryDTO == null
                || sysStudentMatchDeptQueryDTO.getStudentUserIds() == null
                || sysStudentMatchDeptQueryDTO.getStudentUserIds().isEmpty()) {
            return deptMapVO;
        }

        Map<String, List<Long>> deptMap = new HashMap<>();
        List<SysStudentMatchDeptItemVO> bindings =
                sysStudentMatchMapper.selectStudentDeptBindings(sysStudentMatchDeptQueryDTO);
        if (bindings != null) {
            for (SysStudentMatchDeptItemVO binding : bindings) {
                if (binding.getStudentUserId() != null && binding.getDepartmentId() != null) {
                    deptMap.computeIfAbsent(binding.getStudentUserId(), k -> new ArrayList<>())
                           .add(binding.getDepartmentId());
                }
            }
        }
        deptMapVO.setStudentDeptMap(deptMap);
        return deptMapVO;
    }

    @Override
    public String resolveStudentUserIdForMatch(SysStudentMatchVO matchVO) {
        if (matchVO == null || !StringUtils.hasText(matchVO.getUserId())) {
            return null;
        }
        List<SysSchoolFamilyContact> contacts = schoolFamilyContactMapper.selectByParentUserIds(
                Collections.singletonList(matchVO.getUserId().trim()));
        if (contacts == null || contacts.isEmpty()) {
            return null;
        }
        if (StringUtils.hasText(matchVO.getIdName())) {
            String idNameTraditional = ZhConverterUtil.toTraditional(matchVO.getIdName().trim());
            for (SysSchoolFamilyContact contact : contacts) {
                if (!StringUtils.hasText(contact.getStudentUserId()) || !StringUtils.hasText(contact.getStudentName())) {
                    continue;
                }
                String contactNameTraditional = ZhConverterUtil.toTraditional(contact.getStudentName().trim());
                if (idNameTraditional.equals(contactNameTraditional)) {
                    return contact.getStudentUserId().trim();
                }
            }
        }
        for (SysSchoolFamilyContact contact : contacts) {
            if (StringUtils.hasText(contact.getStudentUserId())) {
                return contact.getStudentUserId().trim();
            }
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOneSyncResult(SysStudentMatchSyncRecordDTO syncRecordDTO) {
        if (syncRecordDTO == null || !"1".equals(syncRecordDTO.getSyncStatus())) {
            return;
        }
        if (!StringUtils.hasText(syncRecordDTO.getStudentUserId())
                || !StringUtils.hasText(syncRecordDTO.getSyncTargetName())) {
            return;
        }
        SysWecomStudentNameUpdateDTO sysWecomStudentNameUpdateDTO = new SysWecomStudentNameUpdateDTO();
        sysWecomStudentNameUpdateDTO.setStudentUserId(syncRecordDTO.getStudentUserId());
        sysWecomStudentNameUpdateDTO.setStudentName(syncRecordDTO.getSyncTargetName().trim());
        sysStudentMatchMapper.updateWecomStudentName(sysWecomStudentNameUpdateDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysStudentMatchOperationResultVO syncData(SysStudentMatchSyncDataDTO syncDataDTO) {
        // 查詢未匹配學生列表
        List<SysStudentMatchVO> unmatchedList = sysStudentMatchMapper.selectUnmatchedList(
                new SysStudentMatchDTO(), studentProfilesDatabase());
        if (unmatchedList == null || unmatchedList.isEmpty()) {
            return SysStudentMatchOperationResultVO.success("暫無未匹配的學生數據，無需執行數據同步匹配！");
        }

        // 查詢本地家校通訊錄企微學生數據
        List<SysSchoolFamilyContactVO> contacts = schoolFamilyContactMapper.selectSchoolFamilyContactWithClassList();
        if (contacts == null || contacts.isEmpty()) {
            return SysStudentMatchOperationResultVO.success("本地家校通訊錄中未找到企微學生數據，無法執行自動比對！");
        }

        // 逐條比對未匹配學籍與家校通訊錄：班級 + 姓名一致則寫入匹配記錄
        int matchedCount = 0;
        for (SysStudentMatchVO matchVO : unmatchedList) {
            // student_id 來源：student_profiles.student_info
            String studentId = resolveStudentId(matchVO);
            if (studentId == null) {
                continue;
            }

            // 學籍側比對條件：班級、姓名（缺一不可）
            String classSection = matchVO.getClassSection() != null ? matchVO.getClassSection().trim() : "";
            String idName = matchVO.getIdName() != null ? matchVO.getIdName().trim() : "";
            if (classSection.isEmpty() || idName.isEmpty()) {
                continue;
            }
            // 姓名統一轉繁體，避免簡繁體差異導致匹配失敗
            String idNameTraditional = ZhConverterUtil.toTraditional(idName);

            // 在家校通訊錄中查找：企微班級代碼 + 學生姓名 與學籍一致的首條記錄
            Optional<SysSchoolFamilyContactVO> matchedOpt = contacts.stream().filter(contact -> {
                String contactClass = contact.getClassCodeWecom();
                String contactName = contact.getStudentName();
                // 必須有家長 user_id，且班級、姓名欄位完整
                if (!StringUtils.hasText(contact.getParentUserId())
                        || contactClass == null || contactName == null) {
                    return false;
                }
                // 班級比對：學籍 class_section ↔ 企微班級代碼 class_code_wecom
                if (!contactClass.trim().equalsIgnoreCase(classSection)) {
                    return false;
                }
                // 姓名比對：雙方均轉繁體後精確相等
                String contactNameTraditional = ZhConverterUtil.toTraditional(contactName.trim());
                return contactNameTraditional.equals(idNameTraditional);
            }).findFirst();

            // 匹配成功：寫入 sys_student_match（student_id + 家長 parent_user_id）
            if (matchedOpt.isPresent()) {
                SysSchoolFamilyContactVO contact = matchedOpt.get();
                SysStudentMatch studentMatch = new SysStudentMatch();
                studentMatch.setStudentId(studentId);
                // user_id 存家長 parent_user_id，非企微學生 user_id
                studentMatch.setUserId(contact.getParentUserId());
                // 1 = 自動匹配成功
                studentMatch.setMatchStatus("1");
                if (saveMatchedRecord(studentMatch)) {
                    matchedCount++;
                }
            }
        }

        return SysStudentMatchOperationResultVO.success(
                String.format("同步數據對照完成！共成功自動匹配 %d 筆數據", matchedCount),
                matchedCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysStudentMatchOperationResultVO deleteSysStudentMatchByIds(SysStudentMatchDeleteDTO studentMatchDeleteDTO) {
        if (studentMatchDeleteDTO == null
                || studentMatchDeleteDTO.getMatchIds() == null
                || studentMatchDeleteDTO.getMatchIds().isEmpty()) {
            return SysStudentMatchOperationResultVO.failure("請選擇要刪除的匹配記錄！");
        }
        int rows = sysStudentMatchMapper.deleteSysStudentMatchByIds(studentMatchDeleteDTO);
        return rows > 0
                ? SysStudentMatchOperationResultVO.success("刪除成功", rows)
                : SysStudentMatchOperationResultVO.failure("刪除失敗");
    }

    private String resolveStudentId(SysStudentMatchVO matchVO) {
        if (matchVO == null || !StringUtils.hasText(matchVO.getStudentId())) {
            return null;
        }
        return matchVO.getStudentId().trim();
    }
}
