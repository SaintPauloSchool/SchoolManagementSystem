package com.sms.framework.config;

import com.sms.framework.security.JwtAuthenticationEntryPoint;
import com.sms.framework.security.JwtAuthenticationFilter;
import com.sms.framework.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security配置類
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter
{
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 密碼編碼器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 認證管理器
     */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    /**
     * 配置認證管理器
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    /**
     * 配置安全策略
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // 關閉CSRF
            .csrf().disable()
            // 不創建session
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // 異常處理
            .exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .and()
            // 授權配置
            .authorizeRequests()
            // 公開訪問的資源
            .antMatchers("/favicon.ico", "/sp.png").permitAll()
            .antMatchers("/html/**", "/css/**", "/docs/**", "/fonts/**", "/img/**", "/ajax/**", "/js/**", "/sp/**").permitAll()
            .antMatchers("/captcha/captchaImage**").permitAll()
            .antMatchers("/login", "/register").permitAll()
            // 文件上傳不需要 JWT token，但仍受 ApiSignatureInterceptor 驗簽保護
            .antMatchers("/common/upload").permitAll()
            // 文件下載不需要 JWT token，亦不受驗簽保護
            .antMatchers("/common/download/**").permitAll()
            // 學生相冊靜態資源不需要 JWT token 驗證
            .antMatchers("/studentPhotos/**").permitAll()
            // 其他請求都需要認證
            .anyRequest().authenticated();

        // 添加JWT過濾器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // 禁用緩存
        http.headers().cacheControl();
    }

}
