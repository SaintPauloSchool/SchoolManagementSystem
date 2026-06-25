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
        List<SysSchoolDepartmentMember> sysSchoolDepartmentMemberList = new ArrayList<>();
        for (SysSchoolDepartmentMemberSaveDTO sysSchoolDepartmentMemberSaveDTO : sysSchoolDepartmentMemberBatchSaveDTO.getMembers()) {
            if (Integer.valueOf(2).equals(defaultType)
                    && !StringUtils.hasText(sysSchoolDepartmentMemberSaveDTO.getStudentUserId())) {
                String memberName = sysSchoolDepartmentMemberSaveDTO.getName();
                throw new ServiceException(String.format(
                        "自定義家校成員必須關聯學生：%s",
                        StringUtils.hasText(memberName) ? memberName : sysSchoolDepartmentMemberSaveDTO.getUserid()));
            }
            SysSchoolDepartmentMember sysSchoolDepartmentMember = BeanCopyUtils.copy(sysSchoolDepartmentMemberSaveDTO, SysSchoolDepartmentMember.class);
            sysSchoolDepartmentMember.setType(defaultType);
            sysSchoolDepartmentMemberList.add(sysSchoolDepartmentMember);
        }
        
        // 1. 獲取本次要添加人員所涉及的所有部門 ID
        List<Long> departmentIds = sysSchoolDepartmentMemberList.stream()
                .map(SysSchoolDepartmentMember::getDepartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 2. 查出這些部門下已經存在的人員
        List<SysSchoolDepartmentMember> existingMembers = memberMapper.selectMembersByDepartmentIds(departmentIds);

        // 3. 過濾掉已經在該部門存在的人員
        List<SysSchoolDepartmentMember> toInsert = sysSchoolDepartmentMemberList.stream()
                .filter(m -> existingMembers.stream().noneMatch(exist -> 
                        exist.getDepartmentId().equals(m.getDepartmentId()) && 
                        exist.getUserid().equals(m.getUserid())
                ))
                // 順便做個去重，防止前端傳來的 members 列表裏有重複對象
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(
                                Comparator.comparing(m -> m.getDepartmentId() + "_" + m.getUserid())
                        )), ArrayList::new));

        // 4. 如果全都被過濾掉了（說明想加的人都已經在了），直接返回成功數量，不報錯
        if (toInsert.isEmpty()) {
            return sysSchoolDepartmentMemberList.size();
        }

        // 5. 插入過濾後的真實增量人員
        memberMapper.batchInsertMembers(toInsert);
        
        // 外部可能依賴返回值判斷是否成功，所以統一回傳原數組大小，製造"全部成功加入"(包括已存在的)的假象
        return sysSchoolDepartmentMemberList.size();
    }

}
