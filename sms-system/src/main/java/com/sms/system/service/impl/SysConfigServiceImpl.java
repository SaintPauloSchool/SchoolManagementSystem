package com.sms.system.service.impl;

import com.sms.system.constant.SysConfigKeys;
import com.sms.system.entity.SysConfig;
import com.sms.system.mapper.SysConfigMapper;
import com.sms.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
}
