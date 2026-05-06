package com.sms.framework.security;

import com.sms.common.web.domain.LoginUser;
import com.sms.system.entity.SysToken;
import com.sms.system.mapper.SysTokenMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JWT认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private SysTokenMapper sysTokenMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        // 从请求头中获取token
        String token = getToken(request);
        
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 从数据库查询 token 信息
            SysToken sysToken = sysTokenMapper.selectByToken(token);
            
            // 验证 token 是否存在并且未过期
            if (sysToken != null && !sysToken.isExpired()) {
                // 安全驗證：校園系統只能由員工 (userType == 2) 訪問
                if (sysToken.getUserType() != null && sysToken.getUserType() == 2) {
                    // 加载用户信息
                    LoginUser loginUser = (LoginUser) userDetailsService.loadUserBySysToken(sysToken);
                    
                    if (loginUser != null) {
                        UsernamePasswordAuthenticationToken authenticationToken = 
                            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    }
                }
            }
        }
        
        chain.doFilter(request, response);
    }

    /**
     * 获取请求中的token
     */
    private String getToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return null;
    }
}
