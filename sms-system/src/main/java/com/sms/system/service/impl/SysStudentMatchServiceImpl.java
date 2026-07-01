package com.sms.system.service.impl;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.sms.common.config.StudentProfilesProperties;
import com.sms.common.core.page.PageDomain;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.core.page.TableSupport;
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
 * <p>學籍資料來自 student_profiles.student_info；匹配結果僅寫入 sys_student_match 中已匹配記錄。</p>
 */
@Service
public class SysStudentMatchServiceImpl implements ISysStudentMatchService {

    @Autowired
    private SysStudentMatchMapper sysStudentMatchMapper;

    @Autowired
    private SysSchoolFamilyContactMapper schoolFamilyContactMapper;

    @Autowired
    private StudentProfilesProperties studentProfilesProperties;

    /** 取得學籍庫庫名，供跨庫 SQL 使用 */
    private String studentProfilesDatabase() {
        return studentProfilesProperties.getDatabase();
    }

    /** 根據匹配記錄 ID 查詢詳情（含學籍資料） */
    private SysStudentMatchVO getMatchVOById(Long matchId) {
        return sysStudentMatchMapper.selectStudentMatchVOById(matchId, studentProfilesDatabase());
    }

    /** 根據學生個人編號查詢已存在的匹配記錄 ID */
    private Long findMatchIdByProfileNum(String studentProfileNum) {
        if (!StringUtils.hasText(studentProfileNum)) {
            return null;
        }
        return sysStudentMatchMapper.selectMatchIdByProfileNum(
                new SysStudentMatchProfileNumDTO(studentProfileNum.trim()));
    }

    /**
     * 保存已匹配記錄：有 id 則按主鍵更新，否則按 student_profile_num 新增或覆蓋
     */
    private boolean saveMatchedRecord(SysStudentMatch studentMatch) {
        if (studentMatch == null || !StringUtils.hasText(studentMatch.getStudentProfileNum())) {
            return false;
        }
        studentMatch.setStudentProfileNum(studentMatch.getStudentProfileNum().trim());
        if (studentMatch.getId() != null) {
            return sysStudentMatchMapper.updateStudentMatch(studentMatch) > 0;
        }
        if (studentMatch.getSyncStatus() == null) {
            studentMatch.setSyncStatus("0");
        }
        return sysStudentMatchMapper.saveOrUpdateStudentMatch(studentMatch) > 0;
    }

    /** 將查詢姓名轉為簡繁體，供企微候選列表篩選使用 */
    private void prepareWecomNameQuery(SysWecomStudentDTO wecomStudentDTO) {
        if (wecomStudentDTO == null || !StringUtils.hasText(wecomStudentDTO.getQueryName())) {
            return;
        }
        String queryName = wecomStudentDTO.getQueryName().trim();
        wecomStudentDTO.setQueryName(queryName);
        wecomStudentDTO.setQueryNameTraditional(ZhConverterUtil.toTraditional(queryName));
        wecomStudentDTO.setQueryNameSimplified(ZhConverterUtil.toSimple(queryName));
    }

    /**
     * 查詢學生匹配列表（關聯學籍庫與匹配表）
     */
    @Override
    public List<SysStudentMatchVO> selectSysStudentMatchList(SysStudentMatchDTO studentMatchDTO) {
        return sysStudentMatchMapper.selectSysStudentMatchList(studentMatchDTO, studentProfilesDatabase());
    }

    /**
     * 查詢未匹配學生列表（學籍庫有資料、匹配表無記錄）
     */
    @Override
    public List<SysStudentMatchVO> selectUnmatchedList(SysStudentMatchDTO studentMatchDTO) {
        return sysStudentMatchMapper.selectUnmatchedList(studentMatchDTO, studentProfilesDatabase());
    }

    /**
     * 查詢企微學生候選列表：載入家長學生關係後在業務層篩選、分頁
     */
    @Override
    public TableDataInfo selectWecomCandidates(SysWecomStudentDTO wecomStudentDTO) {
        SysWecomStudentDTO query = wecomStudentDTO != null ? wecomStudentDTO : new SysWecomStudentDTO();
        prepareWecomNameQuery(query);

        Set<String> matchedUserIds = new HashSet<>(sysStudentMatchMapper.selectMatchedWecomUserIds());
        List<SysSchoolFamilyContactVO> filtered = dedupeWecomStudents(schoolFamilyContactMapper.selectSchoolFamilyContactWithClassList())
                .stream()
                .filter(student -> StringUtils.hasText(student.getStudentUserId()))
                .filter(student -> !matchedUserIds.contains(student.getStudentUserId()))
                .filter(student -> matchesWecomCandidate(student, query))
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

    /** 按企微 studentUserId 去重，保留首次出現的記錄 */
    private List<SysSchoolFamilyContactVO> dedupeWecomStudents(List<SysSchoolFamilyContactVO> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, SysSchoolFamilyContactVO> studentMap = new LinkedHashMap<>();
        for (SysSchoolFamilyContactVO item : source) {
            if (!StringUtils.hasText(item.getStudentUserId())) {
                continue;
            }
            studentMap.putIfAbsent(item.getStudentUserId(), item);
        }
        return new ArrayList<>(studentMap.values());
    }

    /** 判斷企微候選學生是否符合姓名、手機、班級篩選條件 */
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

    /** 判斷企微學生姓名是否匹配查詢關鍵字（支持簡繁體） */
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

    /**
     * 手動綁定學籍學生與企微學生，寫入匹配記錄（matchStatus=2）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysStudentMatchOperationResultVO bindStudent(SysStudentMatchBindDTO studentMatchBindDTO) {
        if (studentMatchBindDTO == null
                || !StringUtils.hasText(studentMatchBindDTO.getStudentUserIdWecom())) {
            return SysStudentMatchOperationResultVO.failure(
                    "參數錯誤，請確認 matchId / studentProfileNum 和 studentUserIdWecom 是否為空");
        }

        Long matchId = studentMatchBindDTO.getMatchId();
        String studentProfileNum = studentMatchBindDTO.getStudentProfileNum();
        if (matchId == null && !StringUtils.hasText(studentProfileNum)) {
            return SysStudentMatchOperationResultVO.failure(
                    "參數錯誤，請確認 matchId / studentProfileNum 和 studentUserIdWecom 是否為空");
        }

        if (!StringUtils.hasText(studentProfileNum) && matchId != null) {
            SysStudentMatchVO matchVO = getMatchVOById(matchId);
            studentProfileNum = resolveProfileNum(matchVO);
        }
        if (!StringUtils.hasText(studentProfileNum)) {
            return SysStudentMatchOperationResultVO.failure("綁定失敗，缺少學生個人編號");
        }
        if (matchId == null) {
            matchId = findMatchIdByProfileNum(studentProfileNum);
        }

        String studentUserIdWecom = studentMatchBindDTO.getStudentUserIdWecom().trim();
        List<SysSchoolFamilyContactVO> wecomStudents = schoolFamilyContactMapper.selectSchoolFamilyContactWithClassList();
        String studentNameWecom = studentUserIdWecom;
        for (SysSchoolFamilyContactVO wecomStudent : wecomStudents) {
            if (studentUserIdWecom.equals(wecomStudent.getStudentUserId())) {
                studentNameWecom = wecomStudent.getStudentName();
                break;
            }
        }
        if (!StringUtils.hasText(studentNameWecom)) {
            studentNameWecom = studentUserIdWecom;
        }

        SysStudentMatch studentMatch = new SysStudentMatch();
        studentMatch.setId(matchId);
        studentMatch.setStudentProfileNum(studentProfileNum);
        studentMatch.setStudentUserIdWecom(studentUserIdWecom);
        studentMatch.setStudentNameWecom(studentNameWecom);
        studentMatch.setMatchStatus("2");
        studentMatch.setSyncStatus("0");

        boolean saved = saveMatchedRecord(studentMatch);
        return saved
                ? SysStudentMatchOperationResultVO.success("綁定成功")
                : SysStudentMatchOperationResultVO.failure("綁定失敗");
    }

    /**
     * 查詢待同步至企微的匹配記錄（排除已同步成功的）
     */
    @Override
    public List<SysStudentMatchVO> getPendingListForSync(SysStudentMatchSyncDTO studentMatchSyncDTO) {
        List<SysStudentMatchVO> result = new ArrayList<>();
        if (studentMatchSyncDTO == null || studentMatchSyncDTO.getMatchIds() == null) {
            return result;
        }
        for (Long matchId : studentMatchSyncDTO.getMatchIds()) {
            SysStudentMatchVO matchVO = getMatchVOById(matchId);
            if (matchVO != null && !"1".equals(matchVO.getSyncStatus())) {
                result.add(matchVO);
            }
        }
        return result;
    }

    /**
     * 查詢企微學生與部門的綁定映射（studentUserId → departmentId 列表）
     */
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

    /**
     * 保存單條企微同步結果；同步成功時一併更新家長學生關係表中的企微姓名
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOneSyncResult(SysStudentMatchSyncRecordDTO syncRecordDTO) {
        if (syncRecordDTO == null || syncRecordDTO.getMatchId() == null) {
            return;
        }

        SysStudentMatch studentMatch = new SysStudentMatch();
        studentMatch.setId(syncRecordDTO.getMatchId());
        studentMatch.setSyncStatus(syncRecordDTO.getSyncStatus());
        studentMatch.setErrorMsg(syncRecordDTO.getErrorMsg());
        sysStudentMatchMapper.updateStudentMatch(studentMatch);

        if ("1".equals(syncRecordDTO.getSyncStatus())
                && StringUtils.hasText(syncRecordDTO.getStudentUserIdWecom())
                && StringUtils.hasText(syncRecordDTO.getSyncTargetName())) {
            SysWecomStudentNameUpdateDTO sysWecomStudentNameUpdateDTO = new SysWecomStudentNameUpdateDTO();
            sysWecomStudentNameUpdateDTO.setStudentUserId(syncRecordDTO.getStudentUserIdWecom());
            sysWecomStudentNameUpdateDTO.setStudentName(syncRecordDTO.getSyncTargetName().trim());
            sysStudentMatchMapper.updateWecomStudentName(sysWecomStudentNameUpdateDTO);
        }
    }

    /**
     * 自動比對未匹配學籍與企微學生（優先學生證編號，其次班級+姓名），寫入匹配記錄（matchStatus=1）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysStudentMatchOperationResultVO syncData(SysStudentMatchSyncDataDTO syncDataDTO) {

        List<SysStudentMatchVO> unmatchedList = sysStudentMatchMapper.selectUnmatchedList(
                new SysStudentMatchDTO(), studentProfilesDatabase());
        if (unmatchedList == null || unmatchedList.isEmpty()) {
            return SysStudentMatchOperationResultVO.success("暫無未匹配的學生數據，無需執行數據同步匹配！");
        }

        List<SysSchoolFamilyContactVO> wecomStudents = schoolFamilyContactMapper.selectSchoolFamilyContactWithClassList();
        if (wecomStudents == null || wecomStudents.isEmpty()) {
            return SysStudentMatchOperationResultVO.success("本地關係表中未找到企微學生數據，無法執行自動比對！");
        }

        int matchedCount = 0;
        for (SysStudentMatchVO matchVO : unmatchedList) {
            String profileNum = resolveProfileNum(matchVO);
            if (profileNum == null) {
                continue;
            }

            String classSection = matchVO.getClassSection() != null ? matchVO.getClassSection().trim() : "";
            String idName = matchVO.getIdName() != null ? matchVO.getIdName().trim() : "";
            if (classSection.isEmpty() || idName.isEmpty()) {
                continue;
            }
            String idNameTraditional = ZhConverterUtil.toTraditional(idName);

            Optional<SysSchoolFamilyContactVO> matchedOpt = Optional.empty();
            String dsejIdClean = getDsejStudentIdClean(matchVO);
            if (!dsejIdClean.isEmpty()) {
                matchedOpt = wecomStudents.stream().filter(wecomStudent -> {
                    String wecomUserId = wecomStudent.getStudentUserId();
                    return wecomUserId != null && wecomUserId.trim().equalsIgnoreCase(dsejIdClean);
                }).findFirst();
            }

            if (!matchedOpt.isPresent()) {
                matchedOpt = wecomStudents.stream().filter(wecomStudent -> {
                    String wecomClass = wecomStudent.getClassCodeWecom();
                    String wecomName = wecomStudent.getStudentName();
                    if (wecomClass == null || wecomName == null) {
                        return false;
                    }
                    if (!wecomClass.trim().equalsIgnoreCase(classSection)) {
                        return false;
                    }
                    String wecomNameTraditional = ZhConverterUtil.toTraditional(wecomName.trim());
                    return wecomNameTraditional.equals(idNameTraditional);
                }).findFirst();
            }

            if (matchedOpt.isPresent()) {
                SysSchoolFamilyContactVO wecomStudent = matchedOpt.get();
                SysStudentMatch studentMatch = new SysStudentMatch();
                studentMatch.setStudentProfileNum(profileNum);
                studentMatch.setStudentUserIdWecom(wecomStudent.getStudentUserId());
                studentMatch.setStudentNameWecom(wecomStudent.getStudentName());
                studentMatch.setMatchStatus("1");
                studentMatch.setSyncStatus("0");
                if (saveMatchedRecord(studentMatch)) {
                    matchedCount++;
                }
            }
        }

        return SysStudentMatchOperationResultVO.success(
                String.format("同步數據對照完成！共成功自動匹配 %d 筆數據，請勾選已匹配記錄並點擊「同步至企業微信」進行同步更名。", matchedCount),
                matchedCount);
    }

    /**
     * 批量刪除已匹配記錄
     */
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

    /** 從 VO 解析學生個人編號（優先 studentProfileNum，其次 studentProfileNumber） */
    private String resolveProfileNum(SysStudentMatchVO matchVO) {
        if (matchVO == null) {
            return null;
        }
        if (StringUtils.hasText(matchVO.getStudentProfileNum())) {
            return matchVO.getStudentProfileNum().trim();
        }
        if (matchVO.getStudentProfileNumber() != null) {
            return String.valueOf(matchVO.getStudentProfileNumber());
        }
        return null;
    }

    /** 取得去除橫線後的學生證編號，用於與企微 UserID 比對 */
    private String getDsejStudentIdClean(SysStudentMatchVO matchVO) {
        if (matchVO == null || matchVO.getDsejStudentId() == null) {
            return "";
        }
        return matchVO.getDsejStudentId().replace("-", "").trim();
    }
}
