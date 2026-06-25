package com.sms.system.service.impl.notification;

import com.sms.system.entity.dto.NotificationQuestionSaveDTO;
import com.sms.system.entity.notification.NotificationQuestion;
import com.sms.system.entity.vo.NotificationQuestionVO;
import com.sms.system.mapper.notification.NotificationQuestionMapper;
import com.sms.system.service.notification.INotificationQuestionService;
import com.sms.common.utils.bean.BeanCopyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知問題 Service 業務層處理
 */
@Service
public class NotificationQuestionServiceImpl implements INotificationQuestionService {

    @Autowired
    private NotificationQuestionMapper notificationQuestionMapper;

    @Override
    public List<NotificationQuestionVO> selectByNotificationId(Long notificationId) {
        return BeanCopyUtils.copyList(notificationQuestionMapper.selectByNotificationId(notificationId),
                NotificationQuestionVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int save(NotificationQuestionSaveDTO notificationQuestionSaveDTO) {
        NotificationQuestion question = BeanCopyUtils.copy(notificationQuestionSaveDTO, NotificationQuestion.class);
        if (question.getCreateTime() == null) {
            question.setCreateTime(LocalDateTime.now());
        }
        return notificationQuestionMapper.insert(question);
    }
}
