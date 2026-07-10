package com.sms.system.service.impl;

import com.sms.common.exception.ServiceException;
import com.sms.common.utils.StringUtils;
import com.sms.common.utils.bean.BeanCopyUtils;
import com.sms.system.entity.ClassSection;
import com.sms.system.entity.dto.ClassSectionQueryDTO;
import com.sms.system.entity.dto.ClassSectionSaveDTO;
import com.sms.system.mapper.ClassSectionMapper;
import com.sms.system.service.IClassSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 班級對照 Service 業務層處理
 */
@Service
public class ClassSectionServiceImpl implements IClassSectionService {

    @Autowired
    private ClassSectionMapper classSectionMapper;

    @Override
    public ClassSection getByDsedjName(String classSectionDsedj) {
        return classSectionMapper.selectByDsedjName(classSectionDsedj);
    }

    @Override
    public ClassSection selectClassSectionById(Long id) {
        return classSectionMapper.selectClassSectionById(id);
    }

    @Override
    public List<ClassSection> selectClassSectionList(ClassSectionQueryDTO queryDTO) {
        ClassSection query = BeanCopyUtils.copy(queryDTO, ClassSection.class);
        return classSectionMapper.selectClassSectionList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertClassSection(ClassSectionSaveDTO saveDTO) {
        validateSaveDTO(saveDTO, false);
        checkDuplicate(saveDTO, null);
        ClassSection classSection = BeanCopyUtils.copy(saveDTO, ClassSection.class);
        return classSectionMapper.insertClassSection(classSection);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateClassSection(ClassSectionSaveDTO saveDTO) {
        validateSaveDTO(saveDTO, true);
        checkDuplicate(saveDTO, saveDTO.getId());
        ClassSection classSection = BeanCopyUtils.copy(saveDTO, ClassSection.class);
        return classSectionMapper.updateClassSection(classSection);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteClassSectionByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            throw new ServiceException("請選擇要刪除的記錄");
        }
        return classSectionMapper.deleteClassSectionByIds(ids);
    }

    private void validateSaveDTO(ClassSectionSaveDTO saveDTO, boolean isUpdate) {
        if (saveDTO == null) {
            throw new ServiceException("請求參數不能為空");
        }
        if (isUpdate && saveDTO.getId() == null) {
            throw new ServiceException("ID 不能為空");
        }
        if (StringUtils.isEmpty(saveDTO.getClassSectionDsedj())) {
            throw new ServiceException("DSEDJ 班級名稱不能為空");
        }
        if (StringUtils.isEmpty(saveDTO.getClassSectionSp())) {
            throw new ServiceException("SP 班級代碼不能為空");
        }
        if (saveDTO.getDivision() == null || saveDTO.getDivision() < 0 || saveDTO.getDivision() > 2) {
            throw new ServiceException("請選擇有效的學部");
        }
    }

    private void checkDuplicate(ClassSectionSaveDTO saveDTO, Long excludeId) {
        List<ClassSection> duplicates = classSectionMapper.selectDuplicateList(
                saveDTO.getClassSectionDsedj(),
                saveDTO.getClassSectionSp(),
                excludeId);
        if (duplicates != null && !duplicates.isEmpty()) {
            throw new ServiceException("DSEDJ 班級「" + saveDTO.getClassSectionDsedj()
                    + "」與 SP 班級「" + saveDTO.getClassSectionSp() + "」的對照已存在");
        }
    }
}
