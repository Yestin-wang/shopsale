package com.yestin.web.config;

import com.yestin.common.web.interceptor.FeignRequestInterceptor;
import com.yestin.common.web.interceptor.RequireLoginInterceptor;
import com.yestin.common.web.resolver.UserInfoMethodArgumentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.async.TimeoutCallableProcessingInterceptor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置一个用于异步任务处理的线程池执行器
     * @return 配置好的 ThreadPoolTaskExecutor 实例
     */
    @Bean
    public ThreadPoolTaskExecutor webAsyncThreadPoolExecutor() {
        // 创建一个 ThreadPoolTaskExecutor 实例，用于管理线程池
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        // 设置线程池的核心线程数为 16，即线程池初始创建并保持活动的线程数量
        threadPoolTaskExecutor.setCorePoolSize(16);
        // 设置线程池的最大线程数为 200，当任务队列满时，线程池可以创建的最大线程数量
        threadPoolTaskExecutor.setMaxPoolSize(200);
        // 设置线程的空闲存活时间为 3 秒，当线程空闲超过该时间，会被销毁
        threadPoolTaskExecutor.setKeepAliveSeconds(3);
        // 返回配置好的线程池执行器实例
        return threadPoolTaskExecutor;
    }

    /**
     * 配置 Spring MVC 的异步支持
     * @param configurer 用于配置异步支持的配置器
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 设置异步任务的默认超时时间为 3000 毫秒，即异步任务如果在 3 秒内未完成，将被视为超时
        configurer.setDefaultTimeout(3000);
        // 将之前配置好的线程池执行器设置为异步任务的默认执行器
        configurer.setTaskExecutor(webAsyncThreadPoolExecutor());
    }

    /**
     * 创建一个用于处理 Callable 类型异步任务超时的拦截器
     * @return TimeoutCallableProcessingInterceptor 实例
     */
    @Bean
    public TimeoutCallableProcessingInterceptor timeoutCallableProcessingInterceptor() {
        // 返回一个 TimeoutCallableProcessingInterceptor 实例，用于在异步任务超时时进行相应处理
        return new TimeoutCallableProcessingInterceptor();
    }

    @Bean
    public RequireLoginInterceptor requireLoginInterceptor(StringRedisTemplate redisTemplate){
        return new RequireLoginInterceptor(redisTemplate);
    }
    @Bean
    public FeignRequestInterceptor feignRequestInterceptor(){
        return new FeignRequestInterceptor();
    }
    @Autowired
    private RequireLoginInterceptor requireLoginInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requireLoginInterceptor)
                .addPathPatterns("/**");
    }

    @Bean
    public UserInfoMethodArgumentResolver userInfoMethodArgumentResolver() {
        return new UserInfoMethodArgumentResolver();
    }
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userInfoMethodArgumentResolver());
    }
}
