package com.sms.system.service.impl.notification;

import com.sms.system.entity.WecomSchoolDepartmentMember;
import com.sms.system.mapper.WecomSchoolDepartmentMemberMapper;
import com.sms.system.mapper.notification.NotificationMapper;
import com.sms.system.entity.notification.Notification;
import com.sms.system.service.notification.INotificationCcService;
import com.sms.system.service.notification.INotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sms.system.entity.SysAdmin;
import com.sms.system.mapper.SysAdminMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Autowired
    private SysAdminMapper sysAdminMapper;

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

        // 判斷當前登錄用戶是否為管理員
        boolean isAdmin = false;
        if (openUserId != null && !openUserId.trim().isEmpty()) {
            SysAdmin admin = sysAdminMapper.selectByUserId(openUserId);
            if (admin != null && "0".equals(admin.getStatus())) {
                isAdmin = true;
            }
        }

        Set<Long> notificationIds;
        if (isAdmin) {
            // 如果是管理員，則所有已發佈的通知都默認抄送給他
            Notification query = new Notification();
            query.setStatus("1"); // 已發佈
            List<Notification> allPublished = notificationMapper.selectNotificationList(query);
            notificationIds = allPublished.stream()
                    .map(Notification::getNotificationId)
                    .collect(Collectors.toSet());
        } else {
            // 查詢用戶的部門 ID
            Long departmentId = null;
            if (openUserId != null && !openUserId.trim().isEmpty()) {
                WecomSchoolDepartmentMember member = wecomSchoolDepartmentMemberMapper.selectByUserid(openUserId);
                if (member != null) {
                    departmentId = member.getDepartmentId();
                }
            }
            // 通過 NotificationCcService 查詢抄送給該用戶的所有通知 ID
            notificationIds = notificationCcService.selectNotificationIdsByUserId(userId, departmentId);
        }

        // 如果沒有抄送通知，返回空列表
        if (notificationIds == null || notificationIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 設置通知 ID 列表到通知對象中
        notification.setNotificationIds(notificationIds);

        startPage();
        // 查詢通知詳細信息
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
