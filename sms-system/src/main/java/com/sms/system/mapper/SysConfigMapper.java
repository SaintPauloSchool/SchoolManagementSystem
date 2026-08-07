package com.sms.system.mapper;

import com.sms.system.entity.SysConfig;
import org.apache.ibatis.annotations.Param;

/**
 * 系統配置 Mapper
 */
public interface SysConfigMapper {

    SysConfig selectByConfigKey(@Param("configKey") String configKey);

    int insertConfig(SysConfig config);

    int updateConfigValue(SysConfig config);
}
