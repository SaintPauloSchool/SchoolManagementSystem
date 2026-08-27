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
import com.sms.system.service.ISysSchoolDepartmentService;
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

    @Autowired
    private ISysSchoolDepartmentService schoolDepartmentService;

    @Override
    public List<SysSchoolDepartmentMemberVO> getMembersByDepartmentIds(
            SysSchoolDepartmentMemberQueryDTO sysSchoolDepartmentMemberQueryDTO,
            String ownerUserid) {
        if (sysSchoolDepartmentMemberQueryDTO == null
                || sysSchoolDepartmentMemberQueryDTO.getDepartmentIds() == null
                || sysSchoolDepartmentMemberQueryDTO.getDepartmentIds().isEmpty()) {
            return Collections.emptyList();
        }
        for (Long departmentId : sysSchoolDepartmentMemberQueryDTO.getDepartmentIds()) {
            schoolDepartmentService.assertOwnedBy(departmentId, ownerUserid);
        }
        List<SysSchoolDepartmentMember> sysSchoolDepartmentMemberList =
                memberMapper.selectMembersByDepartmentIds(sysSchoolDepartmentMemberQueryDTO.getDepartmentIds());
        if (sysSchoolDepartmentMemberList == null || sysSchoolDepartmentMemberList.isEmpty()) {
            return Collections.emptyList();
        }
        return BeanCopyUtils.copyList(sysSchoolDepartmentMemberList, SysSchoolDepartmentMemberVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteMemberById(Long id, String ownerUserid) {
        if (id == null) {
            return 0;
        }
        List<SysSchoolDepartmentMember> members = memberMapper.selectMembersByIds(Collections.singletonList(id));
        if (members == null || members.isEmpty() || members.get(0) == null) {
            return 0;
        }
        schoolDepartmentService.assertOwnedBy(members.get(0).getSchoolDepartmentId(), ownerUserid);
        return memberMapper.deleteMemberById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAddMembers(SysSchoolDepartmentMemberBatchSaveDTO sysSchoolDepartmentMemberBatchSaveDTO,
                               String ownerUserid) {
        if (sysSchoolDepartmentMemberBatchSaveDTO == null
                || sysSchoolDepartmentMemberBatchSaveDTO.getMembers() == null
                || sysSchoolDepartmentMemberBatchSaveDTO.getMembers().isEmpty()) {
            return 0;
        }

        Integer defaultType = sysSchoolDepartmentMemberBatchSaveDTO.getType() != null
                ? sysSchoolDepartmentMemberBatchSaveDTO.getType() : 1;
        LocalDateTime now = LocalDateTime.now();
        List<SysSchoolDepartmentMember> sysSchoolDepartmentMemberList = new ArrayList<>();
        for (SysSchoolDepartmentMemberSaveDTO sysSchoolDepartmentMemberSaveDTO
                : sysSchoolDepartmentMemberBatchSaveDTO.getMembers()) {
            if (sysSchoolDepartmentMemberSaveDTO.getSchoolDepartmentId() == null) {
                throw new ServiceException("成員必須指定所屬自定義部門");
            }
            schoolDepartmentService.assertOwnedBy(
                    sysSchoolDepartmentMemberSaveDTO.getSchoolDepartmentId(), ownerUserid);

            if (Integer.valueOf(2).equals(defaultType)
                    && !StringUtils.hasText(sysSchoolDepartmentMemberSaveDTO.getStudentId())) {
                String memberName = sysSchoolDepartmentMemberSaveDTO.getName();
                throw new ServiceException(String.format(
                        "自定義家校成員必須關聯學籍 student_id：%s",
                        StringUtils.hasText(memberName) ? memberName : sysSchoolDepartmentMemberSaveDTO.getUserid()));
            }
            if (Integer.valueOf(2).equals(defaultType)
                    && sysSchoolDepartmentMemberSaveDTO.getDepartmentId() == null) {
                String memberName = sysSchoolDepartmentMemberSaveDTO.getName();
                throw new ServiceException(String.format(
                        "自定義家校成員必須關聯真實班級部門 ID：%s",
                        StringUtils.hasText(memberName) ? memberName : sysSchoolDepartmentMemberSaveDTO.getUserid()));
            }
            SysSchoolDepartmentMember sysSchoolDepartmentMember =
                    BeanCopyUtils.copy(sysSchoolDepartmentMemberSaveDTO, SysSchoolDepartmentMember.class);
            if (StringUtils.hasText(sysSchoolDepartmentMemberSaveDTO.getStudentId())) {
                sysSchoolDepartmentMember.setStudentId(sysSchoolDepartmentMemberSaveDTO.getStudentId().trim());
            }
            sysSchoolDepartmentMember.setType(defaultType);
            sysSchoolDepartmentMember.setCreateTime(now);
            sysSchoolDepartmentMember.setUpdateTime(now);
            sysSchoolDepartmentMemberList.add(sysSchoolDepartmentMember);
        }

        List<Long> schoolDepartmentIds = sysSchoolDepartmentMemberList.stream()
                .map(SysSchoolDepartmentMember::getSchoolDepartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<SysSchoolDepartmentMember> existingMembers =
                memberMapper.selectMembersByDepartmentIds(schoolDepartmentIds);

        List<SysSchoolDepartmentMember> toInsert = sysSchoolDepartmentMemberList.stream()
                .filter(m -> existingMembers.stream().noneMatch(exist ->
                        Objects.equals(exist.getSchoolDepartmentId(), m.getSchoolDepartmentId())
                                && exist.getUserid().equals(m.getUserid())
                                && Objects.equals(exist.getStudentId(), m.getStudentId())
                ))
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(
                                Comparator.comparing(m -> m.getSchoolDepartmentId() + "_"
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
