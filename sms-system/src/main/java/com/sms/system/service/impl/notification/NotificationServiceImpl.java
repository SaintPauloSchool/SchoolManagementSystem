package com.sms.system.service.impl.notification;

import com.sms.system.entity.WecomSchoolDepartmentMember;
import com.sms.system.mapper.WecomSchoolDepartmentMemberMapper;
import com.sms.system.mapper.notification.NotificationMapper;
import com.sms.system.entity.notification.Notification;
import com.sms.system.service.notification.INotificationCcService;
import com.sms.system.service.notification.INotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.sms.common.utils.PageUtils.startPage;

/**
 * 通知 Service 业务层处理
 *
 */
@Service
public class NotificationServiceImpl implements INotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private INotificationCcService notificationCcService;

    @Autowired
    private WecomSchoolDepartmentMemberMapper wecomSchoolDepartmentMemberMapper;

    /**
     * 查询通知列表
     *
     * @param notification 通知信息
     * @return 通知集合
     */
    @Override
    public List<Notification> selectNotificationList(Notification notification) {
        return notificationMapper.selectNotificationList(notification);
    }

    /**
     * 查询通知详细信息
     *
     * @param notificationId 通知主键
     * @return 通知信息
     */
    @Override
    public Notification selectNotificationById(Long notificationId) {
        return notificationMapper.selectById(notificationId);
    }

    /**
     * 根据用户 ID 查询抄送给我的通知列表
     *
     * @param notification 通知信息（包含 userId, openUserId, publishDate）
     * @return 通知集合
     */
    @Override
    public List<Notification> selectCcToMeList(Notification notification) {
        Long userId = notification.getUserId();
        String openUserId = notification.getOpenUserId();
        
        // 查询用户的部门 ID
        Long departmentId = null;
        if (openUserId != null && !openUserId.trim().isEmpty()) {
            WecomSchoolDepartmentMember member = wecomSchoolDepartmentMemberMapper.selectByUserid(openUserId);
            if (member != null) {
                departmentId = member.getDepartmentId();
            }
        }
        
        // 通过 NotificationCcService 查询抄送给该用户的所有通知 ID
        Set<Long> notificationIds = notificationCcService.selectNotificationIdsByUserId(userId, departmentId);
        
        // 如果没有抄送通知，返回空列表
        if (notificationIds == null || notificationIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 设置通知 ID 列表到通知对象中
        notification.setNotificationIds(notificationIds);

        startPage();
        // 查询通知详细信息
        return notificationMapper.selectCcToMeList(notification);
    }

    /**
     * 根据用户 ID 查询我发送的通知列表
     *
     * @param notification 通知信息（包含 senderId, publishDate）
     * @return 通知集合
     */
    @Override
    public List<Notification> selectMySendList(Notification notification) {
        return notificationMapper.selectMySendList(notification);
    }
    
    /**
     * 保存通知
     *
     * @param notification 通知
     * @return 结果
     */
    @Override
    public boolean save(Notification notification) {
        return notificationMapper.insert(notification) > 0;
    }
}
