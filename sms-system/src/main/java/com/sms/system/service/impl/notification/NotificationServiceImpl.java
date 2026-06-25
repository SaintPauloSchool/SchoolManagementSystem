package com.sms.system.service.impl.notification;

import com.sms.common.utils.bean.BeanCopyUtils;
import com.sms.system.entity.SysAdmin;
import com.sms.system.entity.WecomSchoolDepartmentMember;
import com.sms.system.entity.dto.NotificationCcSaveDTO;
import com.sms.system.entity.dto.NotificationQueryDTO;
import com.sms.system.entity.dto.NotificationQuestionSaveDTO;
import com.sms.system.entity.dto.NotificationReceiverSaveDTO;
import com.sms.system.entity.dto.NotificationSaveDTO;
import com.sms.system.entity.notification.Notification;
import com.sms.system.entity.notification.NotificationReceiver;
import com.sms.system.entity.vo.NotificationVO;
import com.sms.system.mapper.SysAdminMapper;
import com.sms.system.mapper.WecomSchoolDepartmentMemberMapper;
import com.sms.system.mapper.notification.NotificationMapper;
import com.sms.system.service.notification.INotificationCcService;
import com.sms.system.service.notification.INotificationQuestionService;
import com.sms.system.service.notification.INotificationReceiverService;
import com.sms.system.service.notification.INotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sms.common.utils.PageUtils.startPage;

/**
 * 通知 Service 業務層處理
 */
@Service
public class NotificationServiceImpl implements INotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private INotificationCcService notificationCcService;

    @Autowired
    private INotificationReceiverService notificationReceiverService;

    @Autowired
    private INotificationQuestionService notificationQuestionService;

    @Autowired
    private WecomSchoolDepartmentMemberMapper wecomSchoolDepartmentMemberMapper;

    @Autowired
    private SysAdminMapper sysAdminMapper;

    /**
     * 查詢通知列表
     */
    @Override
    public List<NotificationVO> selectNotificationList(NotificationQueryDTO notificationQueryDTO) {
        Notification notification = toQueryEntity(notificationQueryDTO);
        return BeanCopyUtils.copyPageList(notificationMapper.selectNotificationList(notification), NotificationVO.class);
    }

    /**
     * 根據 ID 查詢通知
     */
    @Override
    public NotificationVO selectNotificationById(Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        return BeanCopyUtils.copy(notification, NotificationVO.class);
    }

    /**
     * 查詢抄送給我的通知列表
     * <p>管理員可查看全部已發佈通知；普通用戶按抄送配置及所屬部門過濾</p>
     */
    @Override
    public List<NotificationVO> selectCcToMeList(NotificationQueryDTO notificationQueryDTO) {
        Long userId = notificationQueryDTO.getUserId();
        String openUserId = notificationQueryDTO.getOpenUserId();

        // 判斷當前用戶是否為管理員
        boolean isAdmin = false;
        if (openUserId != null && !openUserId.trim().isEmpty()) {
            SysAdmin admin = sysAdminMapper.selectByUserId(openUserId);
            if (admin != null && "0".equals(admin.getStatus())) {
                isAdmin = true;
            }
        }

        Set<Long> notificationIds;
        if (isAdmin) {
            // 管理員：返回所有已發佈通知
            Notification adminQuery = new Notification();
            adminQuery.setStatus("1");
            List<Notification> allPublished = notificationMapper.selectNotificationList(adminQuery);
            notificationIds = allPublished.stream()
                    .map(Notification::getNotificationId)
                    .collect(Collectors.toSet());
        } else {
            // 普通用戶：按抄送對象及部門匹配
            Long departmentId = null;
            if (openUserId != null && !openUserId.trim().isEmpty()) {
                WecomSchoolDepartmentMember member = wecomSchoolDepartmentMemberMapper.selectByUserid(openUserId);
                if (member != null) {
                    departmentId = member.getDepartmentId();
                }
            }
            notificationIds = notificationCcService.selectNotificationIdsByUserId(userId, departmentId);
        }

        if (notificationIds == null || notificationIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 設置通知 ID 列表到通知對象中
        Notification notification = toQueryEntity(notificationQueryDTO);
        notification.setNotificationIds(notificationIds);

        startPage();
        // 查詢通知詳細信息
        return BeanCopyUtils.copyPageList(notificationMapper.selectCcToMeList(notification), NotificationVO.class);
    }

    /**
     * 查詢我發送的通知列表
     */
    @Override
    public List<NotificationVO> selectMySendList(NotificationQueryDTO notificationQueryDTO) {
        Notification notification = toQueryEntity(notificationQueryDTO);
        return BeanCopyUtils.copyPageList(notificationMapper.selectMySendList(notification), NotificationVO.class);
    }

    /**
     * 保存通知（新增或草稿）
     * <p>發佈時會先校驗接收對象，再寫入主表及接收/抄送/問題子表</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(NotificationSaveDTO notificationSaveDTO) {
        Notification notification = BeanCopyUtils.copy(notificationSaveDTO, Notification.class);
        notification.setCreateTime(LocalDateTime.now());

        // 正式發佈前校驗接收對象是否可解析
        if ("1".equals(notificationSaveDTO.getStatus())
                && notificationSaveDTO.getReceivers() != null
                && !notificationSaveDTO.getReceivers().isEmpty()) {
            List<NotificationReceiver> receivers =
                    BeanCopyUtils.copyList(notificationSaveDTO.getReceivers(), NotificationReceiver.class);
            notificationReceiverService.resolveReceivers(receivers, true);
        }

        if (notificationMapper.insert(notification) <= 0) {
            return false;
        }

        Long notificationId = notification.getNotificationId();
        notificationSaveDTO.setNotificationId(notificationId);

        // 保存接收對象
        if (notificationSaveDTO.getReceivers() != null && !notificationSaveDTO.getReceivers().isEmpty()) {
            for (NotificationReceiverSaveDTO notificationReceiverSaveDTO : notificationSaveDTO.getReceivers()) {
                notificationReceiverSaveDTO.setNotificationId(notificationId);
                notificationReceiverService.save(notificationReceiverSaveDTO);
            }
        }

        // 保存抄送對象
        if (notificationSaveDTO.getCcs() != null && !notificationSaveDTO.getCcs().isEmpty()) {
            for (NotificationCcSaveDTO notificationCcSaveDTO : notificationSaveDTO.getCcs()) {
                notificationCcSaveDTO.setNotificationId(notificationId);
                notificationCcService.save(notificationCcSaveDTO);
            }
        }

        // 保存問題列表
        if (notificationSaveDTO.getQuestions() != null && !notificationSaveDTO.getQuestions().isEmpty()) {
            int sortOrder = 1;
            for (NotificationQuestionSaveDTO notificationQuestionSaveDTO : notificationSaveDTO.getQuestions()) {
                notificationQuestionSaveDTO.setNotificationId(notificationId);
                notificationQuestionSaveDTO.setSortOrder(sortOrder++);
                notificationQuestionService.save(notificationQuestionSaveDTO);
            }
        }

        return true;
    }

    /**
     * 撤回通知（僅已發佈狀態可撤回）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recallNotification(Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !"1".equals(notification.getStatus())) {
            return false;
        }

        int rows = notificationMapper.updateStatus(notificationId, "2");
        return rows > 0;
    }

    /**
     * 將查詢 DTO 轉為 Mapper 查詢實體
     */
    private Notification toQueryEntity(NotificationQueryDTO notificationQueryDTO) {
        return BeanCopyUtils.copy(notificationQueryDTO, Notification.class);
    }
}
