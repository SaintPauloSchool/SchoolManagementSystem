package com.sms.system.service.impl;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.sms.common.config.StudentProfilesProperties;
import com.sms.system.entity.SysStudentMatch;
import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.entity.query.SysStudentMatchQuery;
import com.sms.system.entity.vo.SysStudentMatchVO;
import com.sms.system.entity.vo.SysWecomStudentVO;
import com.sms.system.mapper.SysStudentMatchMapper;
import com.sms.system.mapper.SysDepartmentParentBindingMapper;
import com.sms.system.service.ISysStudentMatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SysStudentMatchServiceImpl implements ISysStudentMatchService {

    @Autowired
    private SysStudentMatchMapper sysStudentMatchMapper;

    @Autowired
    private SysDepartmentParentBindingMapper sysDepartmentParentBindingMapper;

    @Autowired
    private StudentProfilesProperties studentProfilesProperties;

    private String studentProfilesDatabase() {
        return studentProfilesProperties.getDatabase();
    }

    private Long ensureMatchRecord(String studentProfileNum) {
        if (studentProfileNum == null || studentProfileNum.trim().isEmpty()) {
            return null;
        }
        String profileNum = studentProfileNum.trim();
        Long matchId = sysStudentMatchMapper.selectMatchIdByProfileNum(profileNum);
        if (matchId != null) {
            return matchId;
        }
        sysStudentMatchMapper.insertMatchRecord(profileNum);
        return sysStudentMatchMapper.selectMatchIdByProfileNum(profileNum);
    }

    private SysStudentMatchVO getMatchVOById(Long id) {
        return sysStudentMatchMapper.selectStudentMatchVOById(id, studentProfilesDatabase());
    }

    @Override
    public List<SysStudentMatchVO> selectSysStudentMatchList(SysStudentMatchQuery query) {
        return sysStudentMatchMapper.selectSysStudentMatchList(query, studentProfilesDatabase());
    }

    @Override
    public List<SysStudentMatchVO> selectUnmatchedList(SysStudentMatchQuery query) {
        return sysStudentMatchMapper.selectUnmatchedList(query, studentProfilesDatabase());
    }

    @Override
    public List<SysWecomStudentVO> selectWecomCandidates(String queryName, String queryMobile, String queryClass) {
        String queryNameTraditional = "";
        String queryNameSimplified = "";
        if (queryName != null && !queryName.trim().isEmpty()) {
            queryName = queryName.trim();
            queryNameTraditional = ZhConverterUtil.toTraditional(queryName);
            queryNameSimplified = ZhConverterUtil.toSimple(queryName);
        }
        return sysStudentMatchMapper.selectWecomCandidates(queryNameTraditional, queryNameSimplified, queryMobile, queryClass);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindStudent(Long matchId, String studentProfileNum, String studentUserIdWecom) {
        if (matchId == null) {
            matchId = ensureMatchRecord(studentProfileNum);
        }
        if (matchId == null || sysStudentMatchMapper.selectMatchById(matchId) == null) {
            return false;
        }

        List<SysWecomStudentVO> wecomStudents = sysStudentMatchMapper.selectWecomStudentInfoList();
        String studentNameWecom = "";
        for (SysWecomStudentVO stu : wecomStudents) {
            if (studentUserIdWecom.equals(stu.getStudentUserId())) {
                studentNameWecom = stu.getStudentName();
                break;
            }
        }
        if (studentNameWecom == null || studentNameWecom.isEmpty()) {
            studentNameWecom = studentUserIdWecom;
        }

        SysStudentMatch match = new SysStudentMatch();
        match.setId(matchId);
        match.setStudentUserIdWecom(studentUserIdWecom);
        match.setStudentNameWecom(studentNameWecom);
        match.setMatchStatus("2");
        return sysStudentMatchMapper.updateSysStudentMatch(match) > 0;
    }

    @Override
    public List<SysStudentMatchVO> getPendingListForSync(List<Long> matchIds) {
        List<SysStudentMatchVO> result = new ArrayList<>();
        for (Long id : matchIds) {
            SysStudentMatchVO m = getMatchVOById(id);
            if (m != null && !"1".equals(m.getSyncStatus())) {
                result.add(m);
            }
        }
        return result;
    }

    @Override
    public Map<String, List<Long>> getStudentDeptMap(List<String> studentUserIds) {
        Map<String, List<Long>> deptMap = new HashMap<>();
        if (studentUserIds == null || studentUserIds.isEmpty()) {
            return deptMap;
        }
        List<SysDepartmentParentBinding> bindings =
                sysDepartmentParentBindingMapper.selectByStudentUserIds(studentUserIds);
        if (bindings != null) {
            for (SysDepartmentParentBinding b : bindings) {
                if (b.getStudentUserId() != null && b.getDepartmentId() != null) {
                    deptMap.computeIfAbsent(b.getStudentUserId(), k -> new ArrayList<>())
                           .add(b.getDepartmentId());
                }
            }
        }
        return deptMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOneSyncResult(SysStudentMatchVO match, String syncStatus, String errorMsg, String operName) {
        SysStudentMatch entity = new SysStudentMatch();
        entity.setId(match.getId());
        entity.setSyncStatus(syncStatus);
        entity.setErrorMsg(errorMsg);
        entity.setUpdateBy(operName);
        sysStudentMatchMapper.updateSysStudentMatch(entity);

        if ("1".equals(syncStatus) && match.getStudentUserIdWecom() != null) {
            String targetName = match.getSyncTargetName();
            if (!targetName.isEmpty()) {
                sysStudentMatchMapper.updateWecomStudentName(match.getStudentUserIdWecom(), targetName);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String syncData(String operName) {
        List<SysStudentMatchVO> unmatchedList = sysStudentMatchMapper.selectUnmatchedList(
                new SysStudentMatchQuery(), studentProfilesDatabase());
        if (unmatchedList == null || unmatchedList.isEmpty()) {
            return "暫無未匹配的學生數據，無需執行數據同步匹配！";
        }

        List<SysWecomStudentVO> wecomStudents = sysStudentMatchMapper.selectWecomStudentInfoList();
        if (wecomStudents == null || wecomStudents.isEmpty()) {
            return "本地關係表中未找到企微學生數據，無法執行自動比對！";
        }

        int matchedCount = 0;
        for (SysStudentMatchVO item : unmatchedList) {
            String profileNum = item.resolveProfileNum();
            if (profileNum == null) {
                continue;
            }

            Long matchId = ensureMatchRecord(profileNum);
            if (matchId == null) {
                continue;
            }

            String classSection = item.getClassSection() != null ? item.getClassSection().trim() : "";
            String idName = item.getIdName() != null ? item.getIdName().trim() : "";
            if (classSection.isEmpty() || idName.isEmpty()) {
                continue;
            }
            String idNameTraditional = ZhConverterUtil.toTraditional(idName);

            Optional<SysWecomStudentVO> matchedOpt = Optional.empty();
            String dsejIdClean = item.getDsejStudentIdClean();
            if (!dsejIdClean.isEmpty()) {
                matchedOpt = wecomStudents.stream().filter(wecomStu -> {
                    String wecomUserId = wecomStu.getStudentUserId();
                    return wecomUserId != null && wecomUserId.trim().equalsIgnoreCase(dsejIdClean);
                }).findFirst();
            }

            if (!matchedOpt.isPresent()) {
                matchedOpt = wecomStudents.stream().filter(wecomStu -> {
                    String wecomClass = wecomStu.getClassCodeWecom();
                    String wecomName = wecomStu.getStudentName();
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
                SysWecomStudentVO wecomStu = matchedOpt.get();
                SysStudentMatch update = new SysStudentMatch();
                update.setId(matchId);
                update.setStudentUserIdWecom(wecomStu.getStudentUserId());
                update.setStudentNameWecom(wecomStu.getStudentName());
                update.setMatchStatus("1");
                update.setUpdateBy(operName);
                sysStudentMatchMapper.updateSysStudentMatch(update);
                matchedCount++;
            }
        }

        return String.format("同步數據對照完成！共成功自動匹配 %d 筆數據，請勾選已匹配記錄並點擊「同步至企業微信」進行同步更名。", matchedCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSysStudentMatchByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return sysStudentMatchMapper.deleteSysStudentMatchByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean clearMatch(Long matchId, String studentProfileNum) {
        if (matchId == null) {
            matchId = ensureMatchRecord(studentProfileNum);
        }
        if (matchId == null) {
            return false;
        }
        return sysStudentMatchMapper.clearStudentMatch(matchId) > 0;
    }
}
