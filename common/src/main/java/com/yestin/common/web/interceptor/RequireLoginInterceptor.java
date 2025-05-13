package com.yestin.common.web.interceptor;

import com.alibaba.fastjson.JSON;
import com.yestin.common.constants.CommonConstants;
import com.yestin.common.domain.UserInfo;
import com.yestin.common.web.CommonCodeMsg;
import com.yestin.common.web.Result;
import com.yestin.common.web.anno.RequireLogin;
import com.yestin.redis.CommonRedisKey;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.*;

public class RequireLoginInterceptor implements HandlerInterceptor {
    private StringRedisTemplate redisTemplate;

    public RequireLoginInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            String feignRequest = request.getHeader(CommonConstants.FEIGN_REQUEST_KEY);
            if (!StringUtils.isEmpty(feignRequest)
                    && CommonConstants.FEIGN_REQUEST_FALSE.equals(feignRequest)
                    && handlerMethod.getMethodAnnotation(RequireLogin.class) != null) {
                // 设置响应格式为json
                response.setContentType("application/json;charset=utf-8");
                // 从请求头中获取token
                String token = request.getHeader(CommonConstants.TOKEN_NAME);
                if (StringUtils.isEmpty(token)) {
                    response.getWriter().write(JSON.toJSONString(Result.error(CommonCodeMsg.TOKEN_INVALID)));
                    return false;
                }
                // 从token中获取redis信息
                UserInfo userInfo = JSON.parseObject(redisTemplate.opsForValue().get(CommonRedisKey.USER_TOKEN.getRealKey(token)), UserInfo.class);
                if (userInfo == null) {
                    response.getWriter().write(JSON.toJSONString(Result.error(CommonCodeMsg.TOKEN_INVALID)));
                    return false;
                }
                // 从请求头中获取请求客户的IP，与上次登陆的用户 ip 做比较，如果不同说明 ip 发生变化，直接响应 ip 改变，需要重新登陆
                String ip = request.getHeader(CommonConstants.REAL_IP);
                if (!userInfo.getLoginIp().equals(ip)) {
                    response.getWriter().write(JSON.toJSONString(Result.error(CommonCodeMsg.LOGIN_IP_CHANGE)));
                    return false;
                }
            }
        }
        return true;
    }
}
