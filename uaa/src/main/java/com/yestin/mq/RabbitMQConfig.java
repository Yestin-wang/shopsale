package com.yestin.mq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 消息转换器
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 登录交换机
    @Bean
    public DirectExchange loginExchange() {
        return ExchangeBuilder.directExchange(MQConstant.LOGIN_EXCHANGE)
                .durable(true)
                .build();
    }

    // 登录成功队列
    @Bean
    public Queue loginQueue() {
        return QueueBuilder.durable(MQConstant.LOGIN_QUEUE)
                .build();
    }

    // 登录失败队列
    @Bean
    public Queue loginFailQueue() {
        return QueueBuilder.durable(MQConstant.LOGIN_QUEUE + ".fail")
                .build();
    }

    // 登录成功绑定
    @Bean
    public Binding bindingLogin() {
        return BindingBuilder.bind(loginQueue())
                .to(loginExchange())
                .with(MQConstant.LOGIN_ROUTING_KEY);
    }

    // 登录失败绑定
    @Bean
    public Binding bindingLoginFail() {
        return BindingBuilder.bind(loginFailQueue())
                .to(loginExchange())
                .with(MQConstant.LOGIN_FAIL_ROUTING_KEY);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
} 