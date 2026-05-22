package com.sms.system.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.entity.SysParentStudentRelation;
import com.sms.system.mapper.SysParentStudentRelationMapper;
import com.sms.system.service.ISysDepartmentParentBindingService;
import com.sms.system.service.ISysParentStudentRelationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 家长学生关系Service业务层处理
 */
@Service
public class SysParentStudentRelationServiceImpl implements ISysParentStudentRelationService {

    private static final Logger logger = LoggerFactory.getLogger(SysParentStudentRelationServiceImpl.class);

    @Autowired
    private SysParentStudentRelationMapper sysParentStudentRelationMapper;

    @Autowired
    private ISysDepartmentParentBindingService departmentParentBindingService;

    /**
     * 创建家长 - 孩子关系记录
     *
     * @param parentUserId     家长用户 ID
     * @param studentUserId    孩子用户 ID
     * @param studentName      孩子姓名
     * @param relation         家长关系描述
     * @param mobile           家长手机号
     * @param externalUserid   家长外部 ID
     */
    @Override
    @Transactional
    public void createAndSaveParentStudentRelation(String parentUserId, String studentUserId, String studentName,
                                                   String relation, String mobile, String externalUserid) {
        // 创建家长学生关系实体
        SysParentStudentRelation relationEntity = new SysParentStudentRelation();
        relationEntity.setParentUserId(parentUserId);
        relationEntity.setStudentUserId(studentUserId);
        relationEntity.setStudentName(studentName);
        relationEntity.setRelationDesc(relation);
        relationEntity.setMobile(mobile);
        relationEntity.setExternalUserid(externalUserid);
        relationEntity.setCreateTime(LocalDateTime.now());
        relationEntity.setUpdateTime(LocalDateTime.now());
        // 插入
        int result = sysParentStudentRelationMapper.insertIgnore(relationEntity);
        if (result > 0) {
            logger.info("创建并保存家长学生关系记录，家长: {}, 学生: {}", parentUserId, studentUserId);
        }
    }

    /**
     * 同步家長學生關係數據
     * 由 ParentStudentRelationSyncTask 調用（每日凌晨 0 點 30 分）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncParentStudentRelationData(Long targetDepartmentId, JSONObject parentJson) {
        // 不存在錯誤編碼則處裡數據
        if (parentJson != null && parentJson.getInteger("errcode") != null && parentJson.getInteger("errcode") == 0) {
            // 獲取家长数据
            JSONArray parentsArray = parentJson.getJSONArray("parents");
            // 家長數據存在，則處裡家长数据
            if (parentsArray != null && !parentsArray.isEmpty()) {
                logger.info("部門 ID {} 成功獲取到 {} 個家長信息", targetDepartmentId, parentsArray.size());
                // 获取当前部门下所有已存在的家长绑定记录
                List<SysDepartmentParentBinding> existingBindings = departmentParentBindingService.selectByDepartmentId(targetDepartmentId);
                // 创建现有记录的映射，便于快速查找
                Map<String, SysDepartmentParentBinding> existingBindingMap = new HashMap<>();
                for (SysDepartmentParentBinding binding : existingBindings) {
                    existingBindingMap.put(binding.getParentUserId(), binding);
                }
                // 创建用于保存的 家长 - 孩子绑定记录
                Set<String> currentParentUserIds = new HashSet<>();
                // 遍历家长数据并保存到数据库
                for (int i = 0; i < parentsArray.size(); i++) {
                    // 获取當前家长数据
                    JSONObject parentObj = parentsArray.getJSONObject(i);
                    String parentUserId = parentObj.getString("parent_userid");
                    String mobile = parentObj.getString("mobile");
                    String externalUserid = parentObj.getString("external_userid");
                    // 记录当前处理的家长 ID
                    currentParentUserIds.add(parentUserId);
                    // 处理孩子信息数组
                    JSONArray childrenArray = parentObj.getJSONArray("children");
                    // 委托给 Service 层处理家长学生关系同步
                    departmentParentBindingService.processParentChildren(targetDepartmentId, parentUserId, childrenArray, existingBindingMap);
                    // 创建并保存家长学生关系记录（如果有孩子的话）
                    if (childrenArray != null && !childrenArray.isEmpty()) {
                        // 遍历孩子信息
                        for (int j = 0; j < childrenArray.size(); j++) {
                            JSONObject childObj = childrenArray.getJSONObject(j);
                            String childStudentUserId = childObj.getString("student_userid");
                            String relation = childObj.getString("relation");
                            String name = childObj.getString("name");
                            // 委托给 Service 层创建家长 - 孩子关系记录
                            createAndSaveParentStudentRelation(parentUserId, childStudentUserId, name, relation, mobile, externalUserid);
                        }
                    }
                }
                // 委托给 Service 层删除不再存在的家长绑定记录
                departmentParentBindingService.deleteObsoleteParentBindings(existingBindings, currentParentUserIds, targetDepartmentId);
            } else {
                logger.info("部門 ID {} 的家長列表為空", targetDepartmentId);
            }
        } else {
            logger.error("獲取部門 ID {} 的家長列表失敗：{}", targetDepartmentId, parentJson != null ? parentJson.getString("errmsg") : "返回結果為空");
        }
    }

}
