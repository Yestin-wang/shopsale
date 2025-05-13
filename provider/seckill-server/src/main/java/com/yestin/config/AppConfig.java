package com.yestin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Configuration
public class AppConfig {
    @Bean
    public ScheduledExecutorService scheduledExecutorService(){
        // 创建线程池
        return new ScheduledThreadPoolExecutor(10);
    }
}
