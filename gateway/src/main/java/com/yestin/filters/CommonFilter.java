package com.yestin.filters;

import com.yestin.common.constants.CommonConstants;
import com.yestin.redis.CommonRedisKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class CommonFilter implements GlobalFilter {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        /**
         * 在请求头中添加客户端真实IP
         * 在请求头中添加FEIGN_REQUEST的请求头，值为0，标记请求不是Feign调用，而是客户端调用
         */
        // 获取客户端真实IP
        String clientIp = getClientIp(exchange.getRequest());
        System.out.println("-----------");
        System.out.println(clientIp);
        ServerHttpRequest request = exchange.getRequest().mutate().
                header(CommonConstants.REAL_IP, clientIp).
                header(CommonConstants.FEIGN_REQUEST_KEY, CommonConstants.FEIGN_REQUEST_FALSE).
                build();
        return chain.filter(exchange.mutate().request(request).build()).then(Mono.fromRunnable(() -> {
            /**
             * 刷新token在redis中的时间
             */
            String token = exchange.getRequest().getHeaders().getFirst(CommonConstants.TOKEN_NAME);
            String redisKey = CommonRedisKey.USER_TOKEN.getRealKey(token);
            if (!StringUtils.isEmpty(token) && redisTemplate.hasKey(redisKey)) {
                redisTemplate.expire(redisKey, CommonRedisKey.USER_TOKEN.getExpireTime(), CommonRedisKey.USER_TOKEN.getUnit());
            }
        }));
    }
    
    /**
     * 获取客户端真实IP地址
     * 按照优先级依次从 X-Forwarded-For, X-Real-IP 请求头获取，如果没有再使用远程地址
     */
    private String getClientIp(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        
        // 优先从 X-Forwarded-For 获取
        List<String> forwardedIps = headers.get("X-Forwarded-For");
        if (forwardedIps != null && !forwardedIps.isEmpty()) {
            // X-Forwarded-For 可能包含多个IP，第一个是客户端真实IP
            String forwardedIp = forwardedIps.get(0);
            if (forwardedIp != null && !forwardedIp.isEmpty()) {
                // 如果包含多个IP，取第一个
                int commaIndex = forwardedIp.indexOf(",");
                if (commaIndex != -1) {
                    return forwardedIp.substring(0, commaIndex).trim();
                }
                return forwardedIp.trim();
            }
        }
        
        // 从 X-Real-IP 获取
        List<String> realIps = headers.get("X-Real-IP");
        if (realIps != null && !realIps.isEmpty() && realIps.get(0) != null && !realIps.get(0).isEmpty()) {
            return realIps.get(0).trim();
        }
        
        // 如果都没有，使用远程地址
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getHostString();
        }
        
        // 默认返回未知
        return "unknown";
    }
}
