package com.sms.framework.security;

import com.sms.common.core.domain.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.sms.system.entity.SysToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 用户验证处理
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 本系統僅支援 Token SSO 登入，不提供傳統帳號密碼登入
        throw new UsernameNotFoundException("不支援帳號密碼登入方式");
    }

    public UserDetails loadUserBySysToken(SysToken sysToken) {
        SysUser user = new SysUser();
        user.setUserId(sysToken.getUserId());
        // Use parentUserId as loginName if available, otherwise use userId
        user.setLoginName(sysToken.getParentUserId() != null ? sysToken.getParentUserId() : String.valueOf(sysToken.getUserId()));
        user.setPassword(""); // token login does not require password validation
        
        return createLoginUser(user);
    }

    public UserDetails createLoginUser(SysUser user) {
        return new LoginUser(user, getUserPermissions(user));
    }

    /**
     * 获取用户权限信息
     */
    public Collection<? extends GrantedAuthority> getUserPermissions(SysUser user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // 管理员拥有所有权限
        if (user.isAdmin()) {
            authorities.add(new SimpleGrantedAuthority("admin"));
            authorities.add(new SimpleGrantedAuthority("*:*:*"));
        } else {
            // 普通用户权限（这里应该从数据库获取）
            authorities.add(new SimpleGrantedAuthority("user:view"));
            authorities.add(new SimpleGrantedAuthority("user:list"));
        }
        
        return authorities;
    }
}
