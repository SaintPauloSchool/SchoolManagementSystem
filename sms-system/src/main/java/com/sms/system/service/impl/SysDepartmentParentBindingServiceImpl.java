package com.sms.system.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.mapper.SysDepartmentParentBindingMapper;
import com.sms.system.service.ISysDepartmentParentBindingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SysDepartmentParentBindingServiceImpl implements ISysDepartmentParentBindingService {

    private static final Logger logger = LoggerFactory.getLogger(SysDepartmentParentBindingServiceImpl.class);

    @Autowired
    private SysDepartmentParentBindingMapper departmentParentBindingMapper;

    @Override
    public List<String> getAllParentUserIds() {
        return departmentParentBindingMapper.selectAllParentUserIds();
    }

    @Override
    public List<SysDepartmentParentBinding> selectByDepartmentId(Long departmentId) {
        return departmentParentBindingMapper.selectByDepartmentId(departmentId);
    }

    @Override
    @Transactional
    public void processParentChildren(Long departmentId, String parentUserId, JSONArray childrenArray,
                                      Map<String, SysDepartmentParentBinding> existingBindingMap) {
        if (childrenArray != null && !childrenArray.isEmpty()) {
            handleParentStudentRelation(
                    departmentId,
                    parentUserId,
                    childrenArray.getJSONObject(0).getString("student_userid"),
                    existingBindingMap
            );
        } else {
            handleParentStudentRelation(departmentId, parentUserId, null, existingBindingMap);
        }
    }

    @Transactional
    public void handleParentStudentRelation(Long departmentId, String parentUserId, String studentUserId,
                                            Map<String, SysDepartmentParentBinding> existingBindingMap) {
        SysDepartmentParentBinding existingBinding = existingBindingMap.get(parentUserId);
        if (existingBinding != null) {
            existingBinding.setStudentUserId(studentUserId);
            existingBinding.setUpdateTime(LocalDateTime.now());
            departmentParentBindingMapper.updateById(existingBinding);
        } else {
            SysDepartmentParentBinding binding = createOrUpdateBinding(departmentId, parentUserId, studentUserId);
            departmentParentBindingMapper.insertIgnore(binding);
        }
    }

    @Override
    @Transactional
    public void deleteObsoleteParentBindings(List<SysDepartmentParentBinding> existingBindings,
                                             Set<String> currentParentUserIds,
                                             Long departmentId) {
        int deletedCount = 0;
        for (SysDepartmentParentBinding binding : existingBindings) {
            if (!currentParentUserIds.contains(binding.getParentUserId())) {
                boolean deleteResult = departmentParentBindingMapper.deleteById(binding.getId()) > 0;
                if (deleteResult) {
                    deletedCount++;
                }
            }
        }
        logger.info("家長數據同步完成，共刪除 {} 個過期的家長綁定", deletedCount);
    }

    private SysDepartmentParentBinding createOrUpdateBinding(Long departmentId, String parentUserId, String studentUserId) {
        SysDepartmentParentBinding binding = new SysDepartmentParentBinding();
        binding.setDepartmentId(departmentId);
        binding.setParentUserId(parentUserId);
        if (studentUserId != null) {
            binding.setStudentUserId(studentUserId);
        }
        binding.setCreateTime(LocalDateTime.now());
        binding.setUpdateTime(LocalDateTime.now());
        return binding;
    }
}
