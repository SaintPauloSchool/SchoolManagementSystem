package com.sms.framework.config;

import com.sms.framework.interceptor.ApiSignatureInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.sms.common.config.OverallSituationConfig;
import com.sms.common.constant.Constants;
import com.sms.framework.interceptor.RepeatSubmitInterceptor;

/**
 * 通用配置
 *
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer
{
    /**
     * 首頁地址
     */
    @Value("${server.servlet.context-path:/}")
    private String indexUrl;

    @Autowired
    private RepeatSubmitInterceptor repeatSubmitInterceptor;

    @Autowired
    private ApiSignatureInterceptor apiSignatureInterceptor;

    /**
     * 默認首頁的設置，當輸入域名是可以自動跳轉到默認指定的網頁
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry)
    {
        registry.addViewController("/").setViewName("forward:" + indexUrl);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        /** 本地文件上傳路徑 */
        registry.addResourceHandler(Constants.RESOURCE_PREFIX + "/**").addResourceLocations("file:" + OverallSituationConfig.getProfile() + "/");

        /** swagger配置 */
        registry.addResourceHandler("/swagger-ui/**").addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/");
    }

    /**
     * 自定義攔截規則
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(repeatSubmitInterceptor).addPathPatterns("/**");
        
        // 籤名校驗攔截器配置
        registry.addInterceptor(apiSignatureInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/captchaImage",
                        "/profile/**",
                        "/favicon.ico",
                        "/common/download/**",
                        "/swagger-ui.html",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/*/api-docs",
                        "/swagger-ui/**",
                        "/tool/swagger",
                        "/tool/swagger/**"
                );
    }
}
