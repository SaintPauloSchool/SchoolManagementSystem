package com.sms.framework.security;

import com.sms.common.core.domain.entity.SysUser;
import com.sms.common.web.domain.LoginUser;
import com.sms.system.entity.WecomSchoolDepartmentMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.sms.system.entity.SysToken;
import com.sms.system.mapper.WecomSchoolDepartmentMemberMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 用户验证处理
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private WecomSchoolDepartmentMemberMapper wecomMemberMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 本系統僅支援 Token SSO 登入，不提供傳統帳號密碼登入
        throw new UsernameNotFoundException("不支援帳號密碼登入方式");
    }

    public UserDetails loadUserBySysToken(SysToken sysToken) {
        // 生成用户信息
        SysUser user = new SysUser();
        // openUserId 存放企業微信原始字串 Controller 透過 getOpenUserId() 取得
        user.setOpenUserId(sysToken.getUserId());
        // loginName 同步存放，歷史相容性
        user.setLoginName(sysToken.getUserId());
        
        // 從 wecom_school_department_member 查詢完整成員對象
        WecomSchoolDepartmentMember member = wecomMemberMapper.selectByUserid(sysToken.getUserId());
        if (member != null) {
            // userId 設置為成員的 id（Long 型）
            user.setUserId(member.getId());
            // userName 設置為成員的 name
            user.setUserName(member.getName());
        } else {
            // 如果找不到成員記錄，設 0L 作為佔位符，使用 userid 作為顯示名稱
            user.setUserId(0L);
            user.setUserName(sysToken.getUserId());
        }
        
        user.setPassword(""); // token login does not require password validation
        // 設置 userType（0:學生 1:家長 2:員工），BaseController.getUserType() 依賴此欄位
        user.setUserType(sysToken.getUserType() != null ? String.valueOf(sysToken.getUserType()) : null);

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
