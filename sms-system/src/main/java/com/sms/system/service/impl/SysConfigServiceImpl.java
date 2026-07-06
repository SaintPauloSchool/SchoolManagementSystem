package com.sms.system.service.impl;

import com.sms.system.constant.SysConfigKeys;
import com.sms.system.entity.SysConfig;
import com.sms.system.mapper.SysConfigMapper;
import com.sms.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysConfigServiceImpl implements ISysConfigService {

    @Autowired
    private SysConfigMapper sysConfigMapper;

    /**
     * 讀取基本設置中配置的家校通訊錄學段部門 ID（type=3）。
     *
     * @return 學段部門 ID，無效配置時返回 null
     */
    @Override
    public Long getAddressBookSegmentDepartmentId() {
        SysConfig config = sysConfigMapper.selectByConfigKey(SysConfigKeys.ADDRESS_BOOK_SEGMENT_DEPT_ID);
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            return null;
        }
        try {
            return Long.parseLong(config.getConfigValue().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAddressBookSegmentDepartmentId(Long segmentDepartmentId, String updateBy) {
        String configValue = segmentDepartmentId != null ? String.valueOf(segmentDepartmentId) : "";

        SysConfig existing = sysConfigMapper.selectByConfigKey(SysConfigKeys.ADDRESS_BOOK_SEGMENT_DEPT_ID);
        if (existing == null) {
            SysConfig config = new SysConfig();
            config.setConfigKey(SysConfigKeys.ADDRESS_BOOK_SEGMENT_DEPT_ID);
            config.setConfigName("家校通訊錄學段");
            config.setConfigValue(configValue);
            config.setConfigGroup("addressbook");
            config.setValueType("number");
            config.setRemark("type=3 學段部門 ID，指定家校通訊錄使用的學段數據");
            config.setCreateBy(updateBy);
            sysConfigMapper.insertConfig(config);
            return;
        }

        SysConfig update = new SysConfig();
        update.setConfigKey(SysConfigKeys.ADDRESS_BOOK_SEGMENT_DEPT_ID);
        update.setConfigValue(configValue);
        update.setUpdateBy(updateBy);
        sysConfigMapper.updateConfigValue(update);
    }

    @Override
    public List<Long> getDailyNoticeClassDepartmentIds() {
        SysConfig config = sysConfigMapper.selectByConfigKey(SysConfigKeys.DAILY_NOTICE_CLASS_DEPT_IDS);
        return parseIdList(config != null ? config.getConfigValue() : null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDailyNoticeClassDepartmentIds(List<Long> classDepartmentIds, String updateBy) {
        String configValue = formatIdList(classDepartmentIds);

        SysConfig existing = sysConfigMapper.selectByConfigKey(SysConfigKeys.DAILY_NOTICE_CLASS_DEPT_IDS);
        if (existing == null) {
            SysConfig config = new SysConfig();
            config.setConfigKey(SysConfigKeys.DAILY_NOTICE_CLASS_DEPT_IDS);
            config.setConfigName("每日學校通知班級範圍");
            config.setConfigValue(configValue);
            config.setConfigGroup("notice");
            config.setValueType("string");
            config.setRemark("type=1 班級部門 ID 列表（逗號分隔），指定每日學校通知發送範圍");
            config.setCreateBy(updateBy);
            sysConfigMapper.insertConfig(config);
            return;
        }

        SysConfig update = new SysConfig();
        update.setConfigKey(SysConfigKeys.DAILY_NOTICE_CLASS_DEPT_IDS);
        update.setConfigValue(configValue);
        update.setUpdateBy(updateBy);
        sysConfigMapper.updateConfigValue(update);
    }

    private List<Long> parseIdList(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : value.split(",")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            try {
                ids.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
                // 跳過無效片段
            }
        }
        return ids;
    }

    private String formatIdList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        return ids.stream()
                .filter(id -> id != null)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
