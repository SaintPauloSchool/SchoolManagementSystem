package com.sms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * 启动程序
 *
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class SmsApplication extends SpringBootServletInitializer
{
    public static void main(String[] args)
    {
        SpringApplication.run(SmsApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application)
    {
        return application.sources(SmsApplication.class);
    }
}
