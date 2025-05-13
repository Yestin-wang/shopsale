package com.yestin.web.config;

import com.yestin.common.web.interceptor.RequireLoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册登录拦截器，拦截所有请求
        registry.addInterceptor(new RequireLoginInterceptor(redisTemplate))
                .addPathPatterns("/**")  // 拦截所有请求
                .excludePathPatterns("/login", "/uaa/login");  // 排除登录接口
    }
    

}
