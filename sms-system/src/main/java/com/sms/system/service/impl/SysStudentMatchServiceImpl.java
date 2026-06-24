package com.sms.system.service.impl;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.sms.common.config.StudentProfilesProperties;
import com.sms.system.entity.dto.*;
import com.sms.system.entity.vo.*;
import com.sms.system.mapper.SysStudentMatchMapper;
import com.sms.system.service.ISysStudentMatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class SysStudentMatchServiceImpl implements ISysStudentMatchService {

    @Autowired
    private SysStudentMatchMapper sysStudentMatchMapper;

    @Autowired
    private StudentProfilesProperties studentProfilesProperties;

    private String studentProfilesDatabase() {
        return studentProfilesProperties.getDatabase();
    }

    private Long ensureMatchRecord(String studentProfileNum) {
        if (!StringUtils.hasText(studentProfileNum)) {
            return null;
        }
        SysStudentMatchProfileNumDTO sysStudentMatchProfileNumDTO = new SysStudentMatchProfileNumDTO(studentProfileNum.trim());
        Long matchId = sysStudentMatchMapper.selectMatchIdByProfileNum(sysStudentMatchProfileNumDTO);
        if (matchId != null) {
            return matchId;
        }

        SysStudentMatchInsertDTO studentMatchInsertDTO = new SysStudentMatchInsertDTO();
        studentMatchInsertDTO.setStudentProfileNum(sysStudentMatchProfileNumDTO.getStudentProfileNum());
        sysStudentMatchMapper.insertMatchRecord(studentMatchInsertDTO);
        return sysStudentMatchMapper.selectMatchIdByProfileNum(sysStudentMatchProfileNumDTO);
    }

    private SysStudentMatchVO getMatchVOById(Long matchId) {
        return sysStudentMatchMapper.selectStudentMatchVOById(matchId, studentProfilesDatabase());
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
    public List<SysWecomStudentVO> selectWecomCandidates(SysWecomStudentDTO wecomStudentDTO) {
        if (wecomStudentDTO == null) {
            wecomStudentDTO = new SysWecomStudentDTO();
        }
        prepareWecomNameQuery(wecomStudentDTO);
        return sysStudentMatchMapper.selectWecomCandidates(wecomStudentDTO);
    }

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

        if (matchId == null) {
            matchId = ensureMatchRecord(studentProfileNum);
        }
        if (matchId == null || sysStudentMatchMapper.countMatchById(matchId) == 0) {
            return SysStudentMatchOperationResultVO.failure("綁定失敗，數據不存在");
        }

        String studentUserIdWecom = studentMatchBindDTO.getStudentUserIdWecom().trim();
        List<SysWecomStudentVO> wecomStudents = sysStudentMatchMapper.selectWecomStudentInfoList();
        String studentNameWecom = studentUserIdWecom;
        for (SysWecomStudentVO wecomStudentVO : wecomStudents) {
            if (studentUserIdWecom.equals(wecomStudentVO.getStudentUserId())) {
                studentNameWecom = wecomStudentVO.getStudentName();
                break;
            }
        }
        if (!StringUtils.hasText(studentNameWecom)) {
            studentNameWecom = studentUserIdWecom;
        }

        SysStudentMatchUpdateDTO studentMatchUpdateDTO = new SysStudentMatchUpdateDTO();
        studentMatchUpdateDTO.setId(matchId);
        studentMatchUpdateDTO.setStudentUserIdWecom(studentUserIdWecom);
        studentMatchUpdateDTO.setStudentNameWecom(studentNameWecom);
        studentMatchUpdateDTO.setMatchStatus("2");
        boolean updated = sysStudentMatchMapper.updateSysStudentMatch(studentMatchUpdateDTO) > 0;
        return updated
                ? SysStudentMatchOperationResultVO.success("綁定成功")
                : SysStudentMatchOperationResultVO.failure("綁定失敗，數據不存在");
    }

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
    @Transactional(rollbackFor = Exception.class)
    public void saveOneSyncResult(SysStudentMatchSyncRecordDTO syncRecordDTO) {
        if (syncRecordDTO == null || syncRecordDTO.getMatchId() == null) {
            return;
        }

        SysStudentMatchUpdateDTO studentMatchUpdateDTO = new SysStudentMatchUpdateDTO();
        studentMatchUpdateDTO.setId(syncRecordDTO.getMatchId());
        studentMatchUpdateDTO.setSyncStatus(syncRecordDTO.getSyncStatus());
        studentMatchUpdateDTO.setErrorMsg(syncRecordDTO.getErrorMsg());
        studentMatchUpdateDTO.setUpdateBy(syncRecordDTO.getOperName());
        sysStudentMatchMapper.updateSysStudentMatch(studentMatchUpdateDTO);

        if ("1".equals(syncRecordDTO.getSyncStatus())
                && StringUtils.hasText(syncRecordDTO.getStudentUserIdWecom())
                && StringUtils.hasText(syncRecordDTO.getSyncTargetName())) {
            SysWecomStudentNameUpdateDTO sysWecomStudentNameUpdateDTO = new SysWecomStudentNameUpdateDTO();
            sysWecomStudentNameUpdateDTO.setStudentUserId(syncRecordDTO.getStudentUserIdWecom());
            sysWecomStudentNameUpdateDTO.setStudentName(syncRecordDTO.getSyncTargetName().trim());
            sysStudentMatchMapper.updateWecomStudentName(sysWecomStudentNameUpdateDTO);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysStudentMatchOperationResultVO syncData(SysStudentMatchSyncDataDTO syncDataDTO) {
        String operName = syncDataDTO != null ? syncDataDTO.getOperName() : null;

        List<SysStudentMatchVO> unmatchedList = sysStudentMatchMapper.selectUnmatchedList(
                new SysStudentMatchDTO(), studentProfilesDatabase());
        if (unmatchedList == null || unmatchedList.isEmpty()) {
            return SysStudentMatchOperationResultVO.success("暫無未匹配的學生數據，無需執行數據同步匹配！");
        }

        List<SysWecomStudentVO> wecomStudents = sysStudentMatchMapper.selectWecomStudentInfoList();
        if (wecomStudents == null || wecomStudents.isEmpty()) {
            return SysStudentMatchOperationResultVO.success("本地關係表中未找到企微學生數據，無法執行自動比對！");
        }

        int matchedCount = 0;
        for (SysStudentMatchVO matchVO : unmatchedList) {
            String profileNum = resolveProfileNum(matchVO);
            if (profileNum == null) {
                continue;
            }

            Long matchId = ensureMatchRecord(profileNum);
            if (matchId == null) {
                continue;
            }

            String classSection = matchVO.getClassSection() != null ? matchVO.getClassSection().trim() : "";
            String idName = matchVO.getIdName() != null ? matchVO.getIdName().trim() : "";
            if (classSection.isEmpty() || idName.isEmpty()) {
                continue;
            }
            String idNameTraditional = ZhConverterUtil.toTraditional(idName);

            Optional<SysWecomStudentVO> matchedOpt = Optional.empty();
            String dsejIdClean = getDsejStudentIdClean(matchVO);
            if (!dsejIdClean.isEmpty()) {
                matchedOpt = wecomStudents.stream().filter(wecomStudentVO -> {
                    String wecomUserId = wecomStudentVO.getStudentUserId();
                    return wecomUserId != null && wecomUserId.trim().equalsIgnoreCase(dsejIdClean);
                }).findFirst();
            }

            if (!matchedOpt.isPresent()) {
                matchedOpt = wecomStudents.stream().filter(wecomStudentVO -> {
                    String wecomClass = wecomStudentVO.getClassCodeWecom();
                    String wecomName = wecomStudentVO.getStudentName();
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
                SysWecomStudentVO wecomStudentVO = matchedOpt.get();
                SysStudentMatchUpdateDTO studentMatchUpdateDTO = new SysStudentMatchUpdateDTO();
                studentMatchUpdateDTO.setId(matchId);
                studentMatchUpdateDTO.setStudentUserIdWecom(wecomStudentVO.getStudentUserId());
                studentMatchUpdateDTO.setStudentNameWecom(wecomStudentVO.getStudentName());
                studentMatchUpdateDTO.setMatchStatus("1");
                studentMatchUpdateDTO.setUpdateBy(operName);
                sysStudentMatchMapper.updateSysStudentMatch(studentMatchUpdateDTO);
                matchedCount++;
            }
        }

        return SysStudentMatchOperationResultVO.success(
                String.format("同步數據對照完成！共成功自動匹配 %d 筆數據，請勾選已匹配記錄並點擊「同步至企業微信」進行同步更名。", matchedCount),
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysStudentMatchOperationResultVO clearMatch(SysStudentMatchClearDTO studentMatchClearDTO) {
        if (studentMatchClearDTO == null) {
            return SysStudentMatchOperationResultVO.failure("參數錯誤，請確認 matchId 或 studentProfileNum 是否為空");
        }

        Long matchId = studentMatchClearDTO.getMatchId();
        String studentProfileNum = studentMatchClearDTO.getStudentProfileNum();
        if (matchId == null && !StringUtils.hasText(studentProfileNum)) {
            return SysStudentMatchOperationResultVO.failure("參數錯誤，請確認 matchId 或 studentProfileNum 是否為空");
        }

        if (matchId == null) {
            matchId = ensureMatchRecord(studentProfileNum);
        }
        if (matchId == null) {
            return SysStudentMatchOperationResultVO.failure("清除匹配失敗，數據不存在");
        }

        boolean cleared = sysStudentMatchMapper.clearStudentMatch(matchId) > 0;
        return cleared
                ? SysStudentMatchOperationResultVO.success("清除匹配成功")
                : SysStudentMatchOperationResultVO.failure("清除匹配失敗，數據不存在");
    }

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

    private String getDsejStudentIdClean(SysStudentMatchVO matchVO) {
        if (matchVO == null || matchVO.getDsejStudentId() == null) {
            return "";
        }
        return matchVO.getDsejStudentId().replace("-", "").trim();
    }
}
