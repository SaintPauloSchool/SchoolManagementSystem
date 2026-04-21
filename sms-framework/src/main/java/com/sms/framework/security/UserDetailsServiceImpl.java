package com.sms.framework.security;

import com.sms.common.core.domain.entity.SysUser;
import com.sms.common.utils.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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
        // 这里应该调用实际的用户服务来获取用户信息
        // 暂时返回一个模拟用户
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setLoginName(username);
        user.setPassword("$2a$10$8K1p/a0dhrxiowP.dnkgNORTWgdEDHn5L2/xjpEWuC.QQv4rKO9jO"); // 123456加密后的密码
        
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
