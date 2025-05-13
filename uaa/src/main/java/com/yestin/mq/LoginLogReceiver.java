package com.yestin.mq;

import com.alibaba.fastjson.JSON;
import com.yestin.domain.LoginLog;
import com.yestin.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoginLogReceiver {
    
    private static final Logger logger = LoggerFactory.getLogger(LoginLogReceiver.class);
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 处理登录成功日志
     * @param loginLog 登录日志对象
     */
    @RabbitListener(queues = MQConstant.LOGIN_QUEUE)
    public void receiveLoginLog(LoginLog loginLog) {
        try {
            logger.info("接收到登录成功日志: {}", loginLog);
            // 将登录日志保存到数据库
            userMapper.insertLoginLog(loginLog);
        } catch (Exception e) {
            logger.error("处理登录日志失败", e);
        }
    }
    
    /**
     * 处理登录失败日志
     * @param loginLog 登录日志对象
     */
    @RabbitListener(queues = MQConstant.LOGIN_QUEUE + ".fail")
    public void receiveLoginFailLog(LoginLog loginLog) {
        try {
            logger.info("接收到登录失败日志: {}, 状态: {}", loginLog, loginLog.getState());
            // 将登录失败日志保存到数据库
            userMapper.insertLoginLog(loginLog);
        } catch (Exception e) {
            logger.error("处理登录失败日志失败", e);
        }
    }
} 