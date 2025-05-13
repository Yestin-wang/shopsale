package com.yestin.mq;

import com.alibaba.fastjson.JSON;
import com.yestin.domain.LoginLog;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoginLogSender {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    /**
     * 发送登录成功日志
     * @param loginLog 登录日志对象
     */
    public void sendLoginSuccessLog(LoginLog loginLog) {
        rabbitTemplate.convertAndSend(MQConstant.LOGIN_EXCHANGE, MQConstant.LOGIN_ROUTING_KEY, loginLog);
    }
    
    /**
     * 发送登录失败日志
     * @param loginLog 登录日志对象
     */
    public void sendLoginFailLog(LoginLog loginLog) {
        rabbitTemplate.convertAndSend(MQConstant.LOGIN_EXCHANGE, MQConstant.LOGIN_FAIL_ROUTING_KEY, loginLog);
    }
} 