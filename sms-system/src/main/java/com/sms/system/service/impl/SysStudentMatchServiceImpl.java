package com.sms.system.service.impl;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.sms.common.config.StudentProfilesProperties;
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
 */
@Service
public class SysStudentMatchServiceImpl implements ISysStudentMatchService {

    @Autowired
    private SysStudentMatchMapper sysStudentMatchMapper;

    @Autowired
    private SysSchoolFamilyContactMapper schoolFamilyContactMapper;

    @Autowired
    private StudentProfilesProperties studentProfilesProperties;

    /**
     *  獲取學籍庫
     *
     * @return 學籍庫
     */
    private String studentProfilesDatabase() {
        return studentProfilesProperties.getDatabase();
    }

    /**
     * 查詢學生匹配列表。
     *
     * @param studentMatchDTO 學生姓名、班級等查詢條件
     * @return 已匹配或待匹配的學生匹配 VO 列表
     */
    @Override
    public List<SysStudentMatchVO> selectSysStudentMatchList(SysStudentMatchDTO studentMatchDTO) {
        return sysStudentMatchMapper.selectSysStudentMatchList(studentMatchDTO, studentProfilesDatabase());
    }

    /**
     * 查詢未匹配學生列表。
     *
     * @param studentMatchDTO 學生姓名、班級等查詢條件
     * @return 未匹配學生 VO 列表
     */
    @Override
    public List<SysStudentMatchVO> selectUnmatchedList(SysStudentMatchDTO studentMatchDTO) {
        return sysStudentMatchMapper.selectUnmatchedList(studentMatchDTO, studentProfilesDatabase());
    }

    /**
     * 查詢企微學生候選列表（手動匹配時選擇企微側學生/家長）。
     *
     * @param wecomStudentDTO 姓名、手機、班級、當前學籍 studentId（排除已綁定家長）等條件
     * @return 企微家校聯絡人候選 VO 列表
     */
    @Override
    public List<SysSchoolFamilyContactVO> selectWecomCandidates(SysWecomStudentDTO wecomStudentDTO) {
        SysWecomStudentDTO query = wecomStudentDTO != null ? wecomStudentDTO : new SysWecomStudentDTO();

        // 姓名：trim 並生成繁/簡體關鍵字，供 Mapper SQL LIKE 匹配
        if (StringUtils.hasText(query.getQueryName())) {
            String queryName = query.getQueryName().trim();
            query.setQueryName(queryName);
            query.setQueryNameTraditional(ZhConverterUtil.toTraditional(queryName));
            query.setQueryNameSimplified(ZhConverterUtil.toSimple(queryName));
        }
        if (StringUtils.hasText(query.getQueryMobile())) {
            query.setQueryMobile(query.getQueryMobile().trim());
        }
        if (StringUtils.hasText(query.getQueryClass())) {
            query.setQueryClass(query.getQueryClass().trim());
        }
        if (StringUtils.hasText(query.getStudentId())) {
            query.setStudentId(query.getStudentId().trim());
        }

        return schoolFamilyContactMapper.selectWecomCandidates(query);
    }

    /**
     * 手動綁定學籍與企微家長。
     *
     * @param studentMatchBindDTO studentId、userId（parent_user_id）
     * @return 綁定是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindStudent(SysStudentMatchBindDTO studentMatchBindDTO) {
        if (studentMatchBindDTO == null
                || !StringUtils.hasText(studentMatchBindDTO.getStudentId())
                || !StringUtils.hasText(studentMatchBindDTO.getUserId())) {
            return false;
        }

        SysStudentMatch studentMatch = new SysStudentMatch();
        studentMatch.setStudentId(
                studentMatchBindDTO.getStudentId().trim()
        );
        studentMatch.setUserId(
                studentMatchBindDTO.getUserId().trim()
        );
        studentMatch.setMatchStatus("2");

        return sysStudentMatchMapper.saveOrUpdateStudentMatch(studentMatch) > 0;
    }


    /**
     * 同步對照數據：按班級 + 姓名自動比對學籍與企微家校通訊錄，寫入匹配記錄。
     *
     * @param syncDataDTO 同步請求（{@code operName} 由控制層注入，當前僅作審計預留）
     * @return 同步結果，含成功匹配筆數
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysStudentMatchOperationResultVO syncData(SysStudentMatchSyncDataDTO syncDataDTO) {
        // 查詢全部學籍學生（每位學生一條，便於為每位家長分別寫入匹配記錄）
        List<SysStudentMatchVO> allStudents = sysStudentMatchMapper.selectAllStudentsForMatch(
                studentProfilesDatabase());
        if (allStudents == null || allStudents.isEmpty()) {
            return SysStudentMatchOperationResultVO.success("暫無學籍學生數據，無需執行數據同步匹配！");
        }

        // 查詢本地家校通訊錄企微學生數據
        List<SysSchoolFamilyContactVO> contacts = schoolFamilyContactMapper.selectSchoolFamilyContactWithClassList();
        if (contacts == null || contacts.isEmpty()) {
            return SysStudentMatchOperationResultVO.success("本地家校通訊錄中未找到企微學生數據，無法執行自動比對！");
        }

        // 已存在的 (student_id, parent_user_id) 組合，避免重複寫入
        Set<String> existingPairKeys = new HashSet<>();
        List<SysStudentMatchVO> existingMatches = sysStudentMatchMapper.selectSysStudentMatchList(
                new SysStudentMatchDTO(), studentProfilesDatabase()
        );
        if (existingMatches != null) {
            for (SysStudentMatchVO matchVO : existingMatches) {
                if (matchVO.getId() != null
                        && StringUtils.hasText(matchVO.getStudentId())
                        && StringUtils.hasText(matchVO.getUserId())) {
                    existingPairKeys.add(
                            matchVO.getStudentId().trim() + "|" + matchVO.getUserId().trim()
                    );
                }
            }
        }

        // 逐條比對學籍與家校通訊錄：班級 + 姓名一致則為每位家長各寫一條匹配記錄
        int matchedCount = 0;
        for (SysStudentMatchVO matchVO : allStudents) {
            String studentId = resolveStudentId(matchVO);
            if (studentId == null) {
                continue;
            }

            String classSection = matchVO.getClassSection() != null ? matchVO.getClassSection().trim() : "";
            String idName = matchVO.getIdName() != null ? matchVO.getIdName().trim() : "";
            if (classSection.isEmpty() || idName.isEmpty()) {
                continue;
            }
            String idNameTraditional = ZhConverterUtil.toTraditional(idName);

            // 在家校通訊錄中查找班級、姓名一致的所有家長（同一學生可有多位家長）
            List<SysSchoolFamilyContactVO> matchedContacts = contacts.stream().filter(contact -> {
                String contactClass = contact.getClassCodeWecom();
                String contactName = contact.getStudentName();
                if (!StringUtils.hasText(contact.getParentUserId())
                        || contactClass == null || contactName == null) {
                    return false;
                }
                if (!contactClass.trim().equalsIgnoreCase(classSection)) {
                    return false;
                }
                String contactNameTraditional = ZhConverterUtil.toTraditional(contactName.trim());
                return contactNameTraditional.equals(idNameTraditional);
            }).collect(Collectors.toList());

            for (SysSchoolFamilyContactVO contact : matchedContacts) {
                String parentUserId = contact.getParentUserId().trim();
                String pairKey = studentId + "|" + parentUserId;
                if (existingPairKeys.contains(pairKey)) {
                    continue;
                }
                SysStudentMatch studentMatch = new SysStudentMatch();
                studentMatch.setStudentId(studentId);
                studentMatch.setUserId(parentUserId);
                studentMatch.setMatchStatus("1");
                if (sysStudentMatchMapper.saveOrUpdateStudentMatch(studentMatch) > 0) {
                    existingPairKeys.add(pairKey);
                    matchedCount++;
                }
            }
        }

        return SysStudentMatchOperationResultVO.success(
                String.format("同步數據對照完成！共成功自動匹配 %d 筆數據", matchedCount),
                matchedCount);
    }

    /**
     * 從匹配 VO 解析學生id
     *
     * @param matchVO 學生匹配 VO
     * @return 學籍 student_id，無效時為 null
     */
    private String resolveStudentId(SysStudentMatchVO matchVO) {
        if (matchVO == null || !StringUtils.hasText(matchVO.getStudentId())) {
            return null;
        }
        return matchVO.getStudentId().trim();
    }

}
