package com.sms.common.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.sms.common.web.domain.LoginUser;
import com.sms.common.core.domain.entity.SysUser;
import com.sms.common.utils.bean.BeanUtils;

/**
 * Spring Security 工具类
 *
 */
public class SecurityUtils
{
    /**
     * 获取Authentication
     */
    public static Authentication getAuthentication()
    {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取用户
     */
    public static LoginUser getLoginUser()
    {
        try
        {
            Object principal = getAuthentication().getPrincipal();
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
     * 获取用户账户
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
     * 获取用户ID
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
     * 生成BCryptPasswordEncoder密码
     */
    public static String encryptPassword(String password)
    {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }

    /**
     * 判断密码是否相同
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword)
    {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 是否为管理员
     */
    public static boolean isAdmin(Long userId)
    {
        return userId != null && 1L == userId;
    }

    /**
     * 获取系统用户
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
     * 设置系统用户
     */
    public static void setSysUser(SysUser user)
    {
        // 在Spring Security中，用户信息通过认证过程设置
        // 这里留空或根据具体需求实现
    }
}
