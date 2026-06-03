package com.sms.system.service.impl.notification;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.WecomSchoolDepartmentMember;
import com.sms.system.entity.SysSchoolDepartmentMember;
import com.sms.system.entity.notification.NotificationCc;
import com.sms.system.mapper.WecomSchoolDepartmentMemberMapper;
import com.sms.system.mapper.SysSchoolDepartmentMemberMapper;
import com.sms.system.mapper.notification.NotificationCcMapper;
import com.sms.system.service.impl.SysSchoolDepartmentServiceImpl;
import com.sms.system.service.impl.WecomSchoolDepartmentServiceImpl;
import com.sms.system.service.notification.INotificationCcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 通知抄送對象 Service 業務層處理
 *
 */
@Service
public class NotificationCcServiceImpl implements INotificationCcService {

    private static final Logger log = LoggerFactory.getLogger(NotificationCcServiceImpl.class);

    @Autowired
    private NotificationCcMapper notificationCcMapper;

    @Autowired
    private WecomSchoolDepartmentMemberMapper wecomSchoolDepartmentMemberMapper;

    @Autowired
    private SysSchoolDepartmentMemberMapper sysSchoolDepartmentMemberMapper;

    @Autowired
    private SysSchoolDepartmentServiceImpl sysSchoolDepartmentService;

    @Autowired
    private WecomSchoolDepartmentServiceImpl wecomSchoolDepartmentService;

    /**
     * 根據通知 ID 查詢抄送對象列表
     *
     * @param notificationId 通知 ID
     * @return 抄送對象集合
     */
    @Override
    public List<NotificationCc> selectByNotificationId(Long notificationId) {
        return notificationCcMapper.selectByNotificationId(notificationId);
    }
    
    /**
     * 新增抄送對象
     *
     * @param cc 抄送對象
     * @return 結果
     */
    @Override
    public int save(NotificationCc cc) {
        return notificationCcMapper.insert(cc);
    }

    /**
     * 解析抄送數據，獲取 userid 列表
     *
     * @param ccs 抄送對象列表
     * @return userid 集合
     */
    @Override
    public Set<String> resolveCcUserIds(List<NotificationCc> ccs) {
        // 初始化 userid 集合
        Set<String> allUserIds = new HashSet<>();
        
        if (ccs == null || ccs.isEmpty()) {
            return allUserIds;
        }

        // 遍歷抄送對象列表
        for (NotificationCc cc : ccs) {
            // 解析抄送數據
            Set<String> userIds = parseCcData(cc);
            // 將 userid 添加到 allUserIds 中
            allUserIds.addAll(userIds);
        }
        
        return allUserIds;
    }

    /**
     * 解析單個抄送對象的數據，獲取 userid 列表
     *
     * @param cc 抄送對象
     * @return userid 集合
     */
    private Set<String> parseCcData(NotificationCc cc) {
        // 初始化 userid 集合
        Set<String> userIds = new HashSet<>();
        
        if (cc.getCcData() == null || cc.getCcData().trim().isEmpty()) {
            return userIds;
        }
        
        try {
            // 解析 cc_data JSON 數組
            JSONArray ccDataArray = JSONObject.parseArray(cc.getCcData());
            if (ccDataArray == null || ccDataArray.isEmpty()) {
                return userIds;
            }
            
            String ccType = cc.getCcType();
            
            for (int i = 0; i < ccDataArray.size(); i++) {
                JSONObject ccItem = ccDataArray.getJSONObject(i);
                if (ccItem == null) {
                    continue;
                }
                
                // 獲取 type：1代表wecom的，2代表自定義的
                Integer type = ccItem.getInteger("type");
                JSONArray ccIds = ccItem.getJSONArray("cc_ids");
                
                if (ccIds == null || ccIds.isEmpty()) {
                    continue;
                }
                
                // 根據 cc_type 和 type 解析 userid
                if ("1".equals(ccType)) {
                    // cc_type = 1 教職員工
                    if (type != null && type == 1) {
                        // type = 1: 用 id 去關聯 wecom_school_department_member 表中的 id 的所有 userid
                        List<Long> ids = ccIds.toJavaList(Long.class);
                        // 用 id 去關聯 wecom_school_department_member 表中的 id 的所有 userid
                        List<WecomSchoolDepartmentMember> members = wecomSchoolDepartmentMemberMapper.selectMembersByIds(ids);
                        //  如果結果非空
                        if (members != null) {
                            // 將 userid 添加到 userIds 中
                            members.stream()
                                .map(WecomSchoolDepartmentMember::getUserid)
                                .filter(userid -> userid != null && !userid.trim().isEmpty())
                                .forEach(userIds::add);
                        }
                    } else if (type != null && type == 2) {
                        // type = 2: 用 id 去關聯 sys_school_department_member 表中的 id 的 type = 1 的 userid
                        List<Long> ids = ccIds.toJavaList(Long.class);
                        // 用 id 去關聯 sys_school_department_member 表中的 id 的 type = 1 的 userid
                        List<SysSchoolDepartmentMember> members = sysSchoolDepartmentMemberMapper.selectMembersByIds(ids);
                        // 如果結果非空
                        if (members != null) {
                            // 將 type = 1 的 userid 添加到 userIds 中
                            members.stream()
                                .filter(m -> m.getType() != null && m.getType() == 1)
                                .map(SysSchoolDepartmentMember::getUserid)
                                .filter(userid -> userid != null && !userid.trim().isEmpty())
                                .forEach(userIds::add);
                        }
                    }
                } else if ("2".equals(ccType)) {
                    // cc_type = 2 學校通訊錄
                    if (type != null && type == 1) {
                        // type = 1: 用 id 去查詢 wecom_school_department_member 表中的 department_id 下的所有 userid
                        List<Long> departmentIds = ccIds.toJavaList(Long.class);
                        // 遞歸獲取所有子孫部門 ID
                        List<Long> allDepartmentIds = wecomSchoolDepartmentService.resolveAllDescendantDepartmentIds(departmentIds);
                        log.info("解析 WeCom 抄送部門 - 輸入部門 IDs: {}, 解析後所有子孫部門 IDs: {}", departmentIds, allDepartmentIds);
                        // 用所有子孫部門 ID 去查詢
                        List<WecomSchoolDepartmentMember> members = wecomSchoolDepartmentMemberMapper.selectMembersByDepartmentIds(allDepartmentIds);
                        // 如果結果非空
                        if (members != null) {
                            // 將所有 userid 添加到 userIds 中
                            members.stream()
                                .map(WecomSchoolDepartmentMember::getUserid)
                                .filter(userid -> userid != null && !userid.trim().isEmpty())
                                .forEach(userIds::add);
                        }
                    } else if (type != null && type == 2) {
                        // type = 2: 用 id 去查詢 sys_school_department_member 表中的 department_id 下的 type = 1 的所有 userid
                        List<Long> departmentIds = ccIds.toJavaList(Long.class);
                        // 遞歸獲取所有子孫部門 ID
                        List<Long> allDepartmentIds = sysSchoolDepartmentService.resolveAllDescendantDepartmentIdsByType(departmentIds, 1);
                        log.info("解析 Sys 抄送部門 - 輸入部門 IDs: {}, 解析後所有子孫部門 IDs: {}", departmentIds, allDepartmentIds);
                        // 用所有子孫部門 ID 去查詢
                        List<SysSchoolDepartmentMember> members = sysSchoolDepartmentMemberMapper.selectMembersByDepartmentIds(allDepartmentIds);
                        // 如果結果非空
                        if (members != null) {
                            // 將所有 type = 1 的 userid 添加到 userIds 中
                            members.stream()
                                .filter(m -> m.getType() != null && m.getType() == 1)
                                .map(SysSchoolDepartmentMember::getUserid)
                                .filter(userid -> userid != null && !userid.trim().isEmpty())
                                .forEach(userIds::add);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析抄送數據失敗: {}", e.getMessage(), e);
        }
        
        return userIds;
    }

    /**
     * 根據用戶 ID 和部門 ID 查詢抄送給該用戶的所有通知 ID 列表
     *
     * @param userId 用戶 ID（用於 cc_type=1）
     * @param departmentId 部門 ID（用於 cc_type=2）
     * @return 通知 ID 集合
     */
    @Override
    public Set<Long> selectNotificationIdsByUserId(Long userId, Long departmentId) {
        Set<Long> notificationIds = new HashSet<>();
        
        // 查詢所有抄送記錄
        List<NotificationCc> allCcList = notificationCcMapper.selectAll();
        
        if (allCcList == null || allCcList.isEmpty()) {
            return notificationIds;
        }
        
        // 遍歷所有抄送記錄，判斷是否包含當前用戶或部門
        for (NotificationCc cc : allCcList) {
            boolean isMatched = false;
            
            if ("1".equals(cc.getCcType())) {
                // cc_type = 1: 教職員工，檢查 cc_ids 中是否包含用戶 ID
                isMatched = checkUserIdInCcData(cc.getCcData(), userId);
            } else if ("2".equals(cc.getCcType()) && departmentId != null) {
                // cc_type = 2: 學校通訊錄，檢查 cc_ids 中是否包含部門 ID
                isMatched = checkDepartmentIdInCcData(cc.getCcData(), departmentId);
            }
            
            if (isMatched) {
                notificationIds.add(cc.getNotificationId());
            }
        }
        
        return notificationIds;
    }

    /**
     * 檢查 cc_data 中是否包含指定的用戶 ID
     */
    private boolean checkUserIdInCcData(String ccData, Long userId) {
        return checkIdInCcData(ccData, userId);
    }

    /**
     * 檢查 cc_data 中是否包含指定的部門 ID
     */
    private boolean checkDepartmentIdInCcData(String ccData, Long departmentId) {
        return checkIdInCcData(ccData, departmentId);
    }

    /**
     * 檢查 cc_data 中是否包含指定的 ID（通用方法）
     *
     * @param ccData 抄送數據 JSON 字符串
     * @param targetId 目標 ID（用戶 ID 或部門 ID）
     * @return 是否包含
     */
    private boolean checkIdInCcData(String ccData, Long targetId) {
        if (ccData == null || ccData.trim().isEmpty() || targetId == null) {
            return false;
        }
        
        try {
            JSONArray ccDataArray = JSONObject.parseArray(ccData);
            if (ccDataArray == null || ccDataArray.isEmpty()) {
                return false;
            }
            
            for (int i = 0; i < ccDataArray.size(); i++) {
                JSONObject ccItem = ccDataArray.getJSONObject(i);
                if (ccItem == null) {
                    continue;
                }
                
                JSONArray ccIds = ccItem.getJSONArray("cc_ids");
                if (ccIds != null && !ccIds.isEmpty()) {
                    List<Long> ids = ccIds.toJavaList(Long.class);
                    if (ids.contains(targetId)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析抄送數據失敗: {}", e.getMessage(), e);
        }
        
        return false;
    }
}
