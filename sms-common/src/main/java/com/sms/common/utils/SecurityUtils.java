package com.sms.common.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.sms.common.web.domain.LoginUser;
import com.sms.common.core.domain.entity.SysUser;

/**
 * Spring Security 工具類
 *
 */
public class SecurityUtils
{
    /**
     * 獲取Authentication
     */
    public static Authentication getAuthentication()
    {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 獲取用戶
     */
    public static LoginUser getLoginUser()
    {
        try
        {
            Authentication authentication = getAuthentication();
            if (authentication == null)
            {
                return null;
            }
            
            Object principal = authentication.getPrincipal();
            if (principal instanceof LoginUser) {
                return (LoginUser) principal;
            }
            return null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * 獲取用戶賬戶
     */
    public static String getUsername()
    {
        try
        {
            return getLoginUser().getUsername();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * 獲取用戶ID
     */
    public static Long getUserId()
    {
        try
        {
            return getLoginUser().getUserId();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * 生成BCryptPasswordEncoder密碼
     */
    public static String encryptPassword(String password)
    {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }

    /**
     * 判斷密碼是否相同
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword)
    {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 是否爲管理員
     */
    public static boolean isAdmin(Long userId)
    {
        return userId != null && 1L == userId;
    }

    /**
     * 獲取系統用戶
     */
    public static SysUser getSysUser()
    {
        LoginUser loginUser = getLoginUser();
        if (loginUser != null)
        {
            return loginUser.getUser();
        }
        return null;
    }

    /**
     * 設置系統用戶
     */
    public static void setSysUser(SysUser user)
    {
        // 在Spring Security中，用戶信息通過認證過程設置
        // 這裡留空或根據具體需求實現
    }
}
