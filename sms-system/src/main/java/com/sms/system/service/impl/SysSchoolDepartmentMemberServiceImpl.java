package com.sms.system.service.impl;

import com.sms.common.exception.ServiceException;
import com.sms.common.utils.bean.BeanCopyUtils;
import com.sms.system.entity.SysSchoolDepartmentMember;
import com.sms.system.entity.dto.SysSchoolDepartmentMemberBatchSaveDTO;
import com.sms.system.entity.dto.SysSchoolDepartmentMemberQueryDTO;
import com.sms.system.entity.dto.SysSchoolDepartmentMemberSaveDTO;
import com.sms.system.entity.vo.SysSchoolDepartmentMemberVO;
import com.sms.system.mapper.SysSchoolDepartmentMemberMapper;
import com.sms.system.service.ISysSchoolDepartmentMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 系統學校部門成員 Service 實現類
 *
 */
@Service
public class SysSchoolDepartmentMemberServiceImpl implements ISysSchoolDepartmentMemberService {

    @Autowired
    private SysSchoolDepartmentMemberMapper memberMapper;

    /**
     * 批量查詢多個部門的成員列表
     */
    @Override
    public List<SysSchoolDepartmentMemberVO> getMembersByDepartmentIds(SysSchoolDepartmentMemberQueryDTO sysSchoolDepartmentMemberQueryDTO) {
        if (sysSchoolDepartmentMemberQueryDTO == null || sysSchoolDepartmentMemberQueryDTO.getDepartmentIds() == null || sysSchoolDepartmentMemberQueryDTO.getDepartmentIds().isEmpty()) {
            return Collections.emptyList();
        }
        List<SysSchoolDepartmentMember> sysSchoolDepartmentMemberList = memberMapper.selectMembersByDepartmentIds(sysSchoolDepartmentMemberQueryDTO.getDepartmentIds());
        if (sysSchoolDepartmentMemberList == null || sysSchoolDepartmentMemberList.isEmpty()) {
            return Collections.emptyList();
        }
        return BeanCopyUtils.copyList(sysSchoolDepartmentMemberList, SysSchoolDepartmentMemberVO.class);
    }

    /**
     * 根據 ID 刪除部門成員
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteMemberById(Long id) {
        if (id == null) {
            return 0;
        }
        return memberMapper.deleteMemberById(id);
    }

    /**
     * 批量添加部門成員 (自動過濾該部門下已存在的 userid)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAddMembers(SysSchoolDepartmentMemberBatchSaveDTO sysSchoolDepartmentMemberBatchSaveDTO) {
        if (sysSchoolDepartmentMemberBatchSaveDTO == null || sysSchoolDepartmentMemberBatchSaveDTO.getMembers() == null || sysSchoolDepartmentMemberBatchSaveDTO.getMembers().isEmpty()) {
            return 0;
        }

        Integer defaultType = sysSchoolDepartmentMemberBatchSaveDTO.getType() != null ? sysSchoolDepartmentMemberBatchSaveDTO.getType() : 1;
        LocalDateTime now = LocalDateTime.now();
        List<SysSchoolDepartmentMember> sysSchoolDepartmentMemberList = new ArrayList<>();
        for (SysSchoolDepartmentMemberSaveDTO sysSchoolDepartmentMemberSaveDTO : sysSchoolDepartmentMemberBatchSaveDTO.getMembers()) {
            if (Integer.valueOf(2).equals(defaultType)
                    && !StringUtils.hasText(sysSchoolDepartmentMemberSaveDTO.getStudentId())) {
                String memberName = sysSchoolDepartmentMemberSaveDTO.getName();
                throw new ServiceException(String.format(
                        "自定義家校成員必須關聯學籍 student_id：%s",
                        StringUtils.hasText(memberName) ? memberName : sysSchoolDepartmentMemberSaveDTO.getUserid()));
            }
            SysSchoolDepartmentMember sysSchoolDepartmentMember = BeanCopyUtils.copy(sysSchoolDepartmentMemberSaveDTO, SysSchoolDepartmentMember.class);
            if (StringUtils.hasText(sysSchoolDepartmentMemberSaveDTO.getStudentId())) {
                sysSchoolDepartmentMember.setStudentId(sysSchoolDepartmentMemberSaveDTO.getStudentId().trim());
            }
            sysSchoolDepartmentMember.setType(defaultType);
            sysSchoolDepartmentMember.setCreateTime(now);
            sysSchoolDepartmentMember.setUpdateTime(now);
            sysSchoolDepartmentMemberList.add(sysSchoolDepartmentMember);
        }
        
        List<Long> departmentIds = sysSchoolDepartmentMemberList.stream()
                .map(SysSchoolDepartmentMember::getDepartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<SysSchoolDepartmentMember> existingMembers = memberMapper.selectMembersByDepartmentIds(departmentIds);

        List<SysSchoolDepartmentMember> toInsert = sysSchoolDepartmentMemberList.stream()
                .filter(m -> existingMembers.stream().noneMatch(exist ->
                        exist.getDepartmentId().equals(m.getDepartmentId())
                                && exist.getUserid().equals(m.getUserid())
                                && Objects.equals(exist.getStudentId(), m.getStudentId())
                ))
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(
                                Comparator.comparing(m -> m.getDepartmentId() + "_"
                                        + m.getUserid() + "_"
                                        + (m.getStudentId() != null ? m.getStudentId() : ""))
                        )), ArrayList::new));

        if (toInsert.isEmpty()) {
            return sysSchoolDepartmentMemberList.size();
        }

        memberMapper.batchInsertMembers(toInsert);
        return sysSchoolDepartmentMemberList.size();
    }

}
