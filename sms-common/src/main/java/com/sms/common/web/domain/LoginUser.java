package com.sms.common.web.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sms.common.core.domain.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * 登錄用戶身份權限
 */
public class LoginUser implements UserDetails {
    
    private static final long serialVersionUID = 1L;

    /**
     * 用戶ID
     */
    private Long userId;

    /**
     * 用戶唯一標識
     */
    private String token;

    /**
     * 登錄時間
     */
    private Long loginTime;

    /**
     * 過期時間
     */
    private Long expireTime;

    /**
     * 登錄IP地址
     */
    private String ipaddr;

    /**
     * 登錄地點
     */
    private String loginLocation;

    /**
     * 瀏覽器類型
     */
    private String browser;

    /**
     * 操作系統
     */
    private String os;

    /**
     * 權限列表
     */
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * 用戶資訊
     */
    private SysUser user;

    public LoginUser() {
    }

    public LoginUser(SysUser user, Collection<? extends GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
        this.userId = user.getUserId();
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return user != null ? user.getPassword() : null;
    }

    @Override
    public String getUsername() {
        return user != null ? user.getLoginName() : null;
    }

    /**
     * 賬戶是否未過期
     */
    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 賬戶是否未鎖定
     */
    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 密碼是否未過期
     */
    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 賬戶是否可用
     */
    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Long loginTime) {
        this.loginTime = loginTime;
    }

    public Long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }

    public String getIpaddr() {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }

    public String getLoginLocation() {
        return loginLocation;
    }

    public void setLoginLocation(String loginLocation) {
        this.loginLocation = loginLocation;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public SysUser getUser() {
        return user;
    }

    public void setUser(SysUser user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "LoginUser{" +
                "userId=" + userId +
                ", username='" + getUsername() + '\'' +
                ", authorities=" + authorities +
                '}';
    }
}
