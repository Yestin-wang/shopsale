package com.yestin.mq;

public class MQConstant {
    // 交换机
    public static final String LOGIN_EXCHANGE = "login.exchange";
    // 队列
    public static final String LOGIN_QUEUE = "login.queue";
    // 路由键
    public static final String LOGIN_ROUTING_KEY = "login.routing.key";
    // 登录失败路由键
    public static final String LOGIN_FAIL_ROUTING_KEY = "login.fail.routing.key";
}
