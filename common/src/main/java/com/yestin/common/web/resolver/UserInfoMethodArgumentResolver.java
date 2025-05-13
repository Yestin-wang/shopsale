package com.yestin.common.web.resolver;

import com.alibaba.fastjson.JSON;
import com.yestin.common.domain.UserInfo;
import com.yestin.redis.CommonRedisKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class UserInfoMethodArgumentResolver implements HandlerMethodArgumentResolver {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 判断参数类型是否要处理
        return parameter.hasParameterAnnotation(RequestUser.class) &&
                parameter.getParameterType() == UserInfo.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest webRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
        // 获取token
        String token = webRequest.getHeader("token");
        return JSON.parseObject(redisTemplate.opsForValue().get(CommonRedisKey.USER_TOKEN.getRealKey(token)), UserInfo.class);
    }
}
