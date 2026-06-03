package com.sms.common.utils.security;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sms.common.constant.PermissionConstants;
import com.sms.common.utils.MessageUtils;

/**
 * permission 工具類
 *
 */
public class PermissionUtils
{
    private static final Logger log = LoggerFactory.getLogger(PermissionUtils.class);

    /**
     * 查看數據的權限
     */
    public static final String VIEW_PERMISSION = "no.view.permission";

    /**
     * 創建數據的權限
     */
    public static final String CREATE_PERMISSION = "no.create.permission";

    /**
     * 修改數據的權限
     */
    public static final String UPDATE_PERMISSION = "no.update.permission";

    /**
     * 刪除數據的權限
     */
    public static final String DELETE_PERMISSION = "no.delete.permission";

    /**
     * 導出數據的權限
     */
    public static final String EXPORT_PERMISSION = "no.export.permission";

    /**
     * 其他數據的權限
     */
    public static final String PERMISSION = "no.permission";

    /**
     * 權限錯誤消息提醒
     * 
     * @param permissionsStr 錯誤信息
     * @return 提示信息
     */
    public static String getMsg(String permissionsStr)
    {
        String permission = StringUtils.substringBetween(permissionsStr, "[", "]");
        String msg = MessageUtils.message(PERMISSION, permission);
        if (StringUtils.endsWithIgnoreCase(permission, PermissionConstants.ADD_PERMISSION))
        {
            msg = MessageUtils.message(CREATE_PERMISSION, permission);
        }
        else if (StringUtils.endsWithIgnoreCase(permission, PermissionConstants.EDIT_PERMISSION))
        {
            msg = MessageUtils.message(UPDATE_PERMISSION, permission);
        }
        else if (StringUtils.endsWithIgnoreCase(permission, PermissionConstants.REMOVE_PERMISSION))
        {
            msg = MessageUtils.message(DELETE_PERMISSION, permission);
        }
        else if (StringUtils.endsWithIgnoreCase(permission, PermissionConstants.EXPORT_PERMISSION))
        {
            msg = MessageUtils.message(EXPORT_PERMISSION, permission);
        }
        else if (StringUtils.endsWithAny(permission,
                new String[] { PermissionConstants.VIEW_PERMISSION, PermissionConstants.LIST_PERMISSION }))
        {
            msg = MessageUtils.message(VIEW_PERMISSION, permission);
        }
        return msg;
    }

    /**
     * 返回用戶屬性值
     *
     * @param property 屬性名稱
     * @return 用戶屬性值
     */
    public static Object getPrincipalProperty(String property)
    {
        // Spring Security implementation would go here
        // For now, returning null as placeholder
        return null;
    }
}
