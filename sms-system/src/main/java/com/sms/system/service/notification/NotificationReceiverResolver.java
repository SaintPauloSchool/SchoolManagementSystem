package com.sms.system.service.notification;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.entity.SysParentStudentRelation;
import com.sms.system.entity.SysSchoolDepartmentMember;
import com.sms.system.entity.notification.NotificationReceiver;
import com.sms.system.mapper.SysDepartmentParentBindingMapper;
import com.sms.system.mapper.SysParentStudentRelationMapper;
import com.sms.system.mapper.SysSchoolDepartmentMemberMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationReceiverResolver {

    private static final Logger log = LoggerFactory.getLogger(NotificationReceiverResolver.class);

    @Autowired
    private SysParentStudentRelationMapper parentStudentRelationMapper;

    @Autowired
    private SysSchoolDepartmentMemberMapper schoolDepartmentMemberMapper;

    @Autowired
    private SysDepartmentParentBindingMapper departmentParentBindingMapper;

    public Map<String, List<String>> resolveReceivers(List<NotificationReceiver> receivers) {
        Set<String> parentUserIds = new HashSet<>();
        Set<String> studentUserIds = new HashSet<>();
        Set<String> partyIds = new HashSet<>();

        if (receivers == null || receivers.isEmpty()) {
            log.warn("notification receivers are empty");
            return buildResult(parentUserIds, studentUserIds, partyIds);
        }

        for (NotificationReceiver receiver : receivers) {
            String receiveType = receiver.getReceiveType();
            String receiveData = receiver.getReceiveData();

            if (receiveData == null || receiveData.trim().isEmpty()) {
                continue;
            }

            try {
                JSONArray dataArray = JSON.parseArray(receiveData);
                if (dataArray == null || dataArray.isEmpty()) {
                    continue;
                }

                for (int i = 0; i < dataArray.size(); i++) {
                    JSONObject dataItem = dataArray.getJSONObject(i);
                    Integer type = dataItem.getInteger("type");
                    JSONArray ids = dataItem.getJSONArray("ids");

                    if (type == null || ids == null || ids.isEmpty()) {
                        continue;
                    }

                    List<Long> idList = ids.toJavaList(Long.class);

                    if ("2".equals(receiveType)) {
                        if (type == 1) {
                            resolveParentUserIds(idList, parentUserIds);
                        } else if (type == 2) {
                            resolveStudentUserIds(idList, studentUserIds);
                        }
                    } else if ("1".equals(receiveType)) {
                        if (type == 1) {
                            resolveParentUserIdsByDepartment(idList, parentUserIds);
                        } else if (type == 2) {
                            resolveStudentUserIdsByDepartment(idList, studentUserIds);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("failed to resolve notification receivers: {}", receiveData, e);
            }
        }

        return buildResult(parentUserIds, studentUserIds, partyIds);
    }

    private void resolveParentUserIds(List<Long> ids, Set<String> parentUserIds) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<SysParentStudentRelation> relations = parentStudentRelationMapper.selectByParentUserIds(
            ids.stream().map(String::valueOf).collect(Collectors.toList())
        );

        if (relations != null) {
            for (SysParentStudentRelation relation : relations) {
                if (relation.getParentUserId() != null && !relation.getParentUserId().trim().isEmpty()) {
                    parentUserIds.add(relation.getParentUserId());
                }
            }
        }
    }

    private void resolveStudentUserIds(List<Long> ids, Set<String> studentUserIds) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<SysSchoolDepartmentMember> members = schoolDepartmentMemberMapper.selectMembersByDepartmentIds(ids);
        if (members != null) {
            for (SysSchoolDepartmentMember member : members) {
                if (member.getUserid() != null && !member.getUserid().trim().isEmpty()) {
                    studentUserIds.add(member.getUserid());
                }
            }
        }
    }

    private void resolveParentUserIdsByDepartment(List<Long> departmentIds, Set<String> parentUserIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return;
        }

        List<SysDepartmentParentBinding> bindings = departmentParentBindingMapper.selectByDepartmentIds(departmentIds);
        if (bindings != null) {
            for (SysDepartmentParentBinding binding : bindings) {
                if (binding.getParentUserId() != null && !binding.getParentUserId().trim().isEmpty()) {
                    parentUserIds.add(binding.getParentUserId());
                }
            }
        }
    }

    private void resolveStudentUserIdsByDepartment(List<Long> departmentIds, Set<String> studentUserIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return;
        }

        List<SysSchoolDepartmentMember> members = schoolDepartmentMemberMapper.selectMembersByDepartmentIds(departmentIds);
        if (members != null) {
            for (SysSchoolDepartmentMember member : members) {
                if (member.getUserid() != null && !member.getUserid().trim().isEmpty()) {
                    studentUserIds.add(member.getUserid());
                }
            }
        }
    }

    private Map<String, List<String>> buildResult(Set<String> parentUserIds, Set<String> studentUserIds, Set<String> partyIds) {
        Map<String, List<String>> result = new HashMap<>();
        result.put("to_parent_userid", new ArrayList<>(parentUserIds));
        result.put("to_student_userid", new ArrayList<>(studentUserIds));
        result.put("to_party", new ArrayList<>(partyIds));
        return result;
    }
}